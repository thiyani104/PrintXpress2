-- =====================================================================================
-- PrintXpress backend for Supabase (Postgres)
-- Run this ONCE: Supabase Dashboard -> SQL Editor -> New query -> paste -> Run.
-- It is safe to run again (it uses IF NOT EXISTS / CREATE OR REPLACE / DROP POLICY IF EXISTS).
--
-- Security model
--   * The mobile app only ever holds the PUBLIC anon/publishable key.
--   * The anon role has NO direct access to any table. It can only call three functions:
--       place_order, get_order_statuses, cancel_order      (all SECURITY DEFINER, validated)
--     and upload files into the private "designs" bucket.
--   * Order ids are random UUIDs generated on the phone, so nobody can guess or list orders.
--   * Shop staff = Supabase Auth users (you create them). They can read everything and may
--     change only: status, shop_notes, cancel_reason.
--   * TURN OFF public sign-ups in Authentication settings, otherwise anyone could become "staff".
-- =====================================================================================

-- ------------------------------------------------------------------ tables
create table if not exists public.orders (
  id               uuid primary key,
  created_at       timestamptz not null default now(),
  updated_at       timestamptz not null default now(),
  status           text not null default 'Pending'
                     check (status in ('Pending','Processing','Printing','Completed','Cancelled')),
  customer_name    text not null check (char_length(customer_name)  between 1 and 120),
  customer_email   text not null check (char_length(customer_email) between 3 and 200),
  customer_phone   text not null check (char_length(customer_phone) between 7 and 20),
  delivery_method  text not null check (delivery_method in ('Home Delivery','Pickup')),
  delivery_address text check (char_length(delivery_address) <= 500),
  delivery_date    date,
  payment_method   text not null
                     check (payment_method in ('Cash on Delivery','Card on Delivery','Bank Transfer')),
  subtotal         numeric(12,2) not null check (subtotal >= 0),
  discount         numeric(12,2) not null default 0 check (discount >= 0),
  promo_code       text check (char_length(promo_code) <= 40),
  delivery_fee     numeric(12,2) not null default 0 check (delivery_fee >= 0),
  total            numeric(12,2) not null check (total >= 0),
  cancel_reason    text check (char_length(cancel_reason) <= 300),
  shop_notes       text check (char_length(shop_notes) <= 1000)
);

create table if not exists public.order_items (
  id            bigint generated always as identity primary key,
  order_id      uuid not null references public.orders(id) on delete cascade,
  product_id    text,
  product_name  text not null check (char_length(product_name) between 1 and 200),
  spec          text,
  quantity      integer not null check (quantity between 1 and 9999),
  unit_price    numeric(12,2) not null check (unit_price >= 0),
  file_path     text,
  notes         text check (char_length(notes) <= 500)
);

create index if not exists order_items_order_id_idx on public.order_items (order_id);
create index if not exists orders_status_created_idx on public.orders (status, created_at desc);

create or replace function public.touch_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

drop trigger if exists orders_touch_updated_at on public.orders;
create trigger orders_touch_updated_at
  before update on public.orders
  for each row execute function public.touch_updated_at();

-- ------------------------------------------------------------------ row level security
alter table public.orders      enable row level security;
alter table public.order_items enable row level security;

drop policy if exists "staff read orders"   on public.orders;
drop policy if exists "staff update orders" on public.orders;
drop policy if exists "staff read items"    on public.order_items;

create policy "staff read orders"   on public.orders
  for select to authenticated using (true);
create policy "staff update orders" on public.orders
  for update to authenticated using (true) with check (true);
create policy "staff read items"    on public.order_items
  for select to authenticated using (true);

-- Table privileges: nothing for anon; staff may read, and update only three columns.
revoke all on public.orders      from anon, authenticated;
revoke all on public.order_items from anon, authenticated;
grant select on public.orders      to authenticated;
grant select on public.order_items to authenticated;
grant update (status, shop_notes, cancel_reason) on public.orders to authenticated;

-- ------------------------------------------------------------------ functions used by the app
-- place_order: validates and stores an order. Idempotent: sending the same id twice is harmless.
create or replace function public.place_order(payload jsonb)
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
  v_id       uuid;
  v_inserted uuid;
  v_item     jsonb;
  v_count    integer;
  v_subtotal numeric(12,2);
  v_discount numeric(12,2);
  v_fee      numeric(12,2);
  v_total    numeric(12,2);
  v_path     text;
begin
  if payload is null or jsonb_typeof(payload) <> 'object' then
    raise exception 'invalid payload';
  end if;
  if coalesce(jsonb_typeof(payload -> 'items'), '') <> 'array' then
    raise exception 'items are required';
  end if;

  v_id := (payload ->> 'id')::uuid;
  if v_id is null then
    raise exception 'missing order id';
  end if;

  v_count := jsonb_array_length(payload -> 'items');
  if v_count < 1 or v_count > 50 then
    raise exception 'invalid number of items';
  end if;

  v_subtotal := (payload ->> 'subtotal')::numeric;
  v_discount := coalesce((payload ->> 'discount')::numeric, 0);
  v_fee      := coalesce((payload ->> 'delivery_fee')::numeric, 0);
  v_total    := (payload ->> 'total')::numeric;
  if v_subtotal is null or v_total is null
     or abs((v_subtotal - v_discount + v_fee) - v_total) > 0.01 then
    raise exception 'totals do not add up';
  end if;

  insert into public.orders (
    id, customer_name, customer_email, customer_phone,
    delivery_method, delivery_address, delivery_date, payment_method,
    subtotal, discount, promo_code, delivery_fee, total
  )
  values (
    v_id,
    payload ->> 'customer_name',
    payload ->> 'customer_email',
    payload ->> 'customer_phone',
    payload ->> 'delivery_method',
    nullif(payload ->> 'delivery_address', ''),
    nullif(payload ->> 'delivery_date', '')::date,
    payload ->> 'payment_method',
    v_subtotal, v_discount,
    nullif(payload ->> 'promo_code', ''),
    v_fee, v_total
  )
  on conflict (id) do nothing
  returning id into v_inserted;

  -- Only a brand-new order gets its items (a retry of an already-stored order does nothing).
  if v_inserted is not null then
    for v_item in select value from jsonb_array_elements(payload -> 'items') loop
      v_path := nullif(v_item ->> 'file_path', '');
      -- A design file must live inside this order's own folder in the bucket.
      if v_path is not null and left(v_path, 37) <> v_id::text || '/' then
        v_path := null;
      end if;

      insert into public.order_items (
        order_id, product_id, product_name, spec, quantity, unit_price, file_path, notes
      )
      values (
        v_id,
        v_item ->> 'product_id',
        v_item ->> 'product_name',
        nullif(v_item ->> 'spec', ''),
        (v_item ->> 'quantity')::integer,
        (v_item ->> 'unit_price')::numeric,
        v_path,
        nullif(v_item ->> 'notes', '')
      );
    end loop;
  end if;

  return v_id;
end;
$$;

-- get_order_statuses: lets the app ask "what is the status of MY orders?" using their secret ids.
create or replace function public.get_order_statuses(ids uuid[])
returns table (id uuid, status text, cancel_reason text)
language sql
security definer
set search_path = public
stable
as $$
  select o.id, o.status, o.cancel_reason
  from public.orders o
  where coalesce(array_length(ids, 1), 0) <= 100
    and o.id = any (ids);
$$;

-- cancel_order: a customer may cancel only while the shop has not started (status Pending).
create or replace function public.cancel_order(p_id uuid, p_reason text default null)
returns boolean
language plpgsql
security definer
set search_path = public
as $$
declare
  v_rows integer;
begin
  update public.orders
     set status = 'Cancelled',
         cancel_reason = left(p_reason, 300)
   where id = p_id
     and status = 'Pending';
  get diagnostics v_rows = row_count;
  return v_rows > 0;
end;
$$;

revoke all on function public.place_order(jsonb)           from public;
revoke all on function public.get_order_statuses(uuid[])   from public;
revoke all on function public.cancel_order(uuid, text)     from public;
grant execute on function public.place_order(jsonb)         to anon, authenticated;
grant execute on function public.get_order_statuses(uuid[]) to anon, authenticated;
grant execute on function public.cancel_order(uuid, text)   to anon, authenticated;

-- ------------------------------------------------------------------ storage for design files
-- Private bucket, 50 MB per file (the free-plan maximum). The app uploads with the anon key
-- (insert only); only signed-in staff can read.
insert into storage.buckets (id, name, public, file_size_limit)
values ('designs', 'designs', false, 52428800)
on conflict (id) do update set public = false, file_size_limit = excluded.file_size_limit;

drop policy if exists "app can upload designs"  on storage.objects;
drop policy if exists "staff can read designs"  on storage.objects;

create policy "app can upload designs" on storage.objects
  for insert to anon with check (bucket_id = 'designs');
create policy "staff can read designs" on storage.objects
  for select to authenticated using (bucket_id = 'designs');

-- ------------------------------------------------------------------ realtime (optional)
-- Lets the shop dashboard update instantly when a new order arrives.
do $$
begin
  alter publication supabase_realtime add table public.orders;
exception
  when duplicate_object then null;
  when undefined_object then null;
end;
$$;
