package com.printxpress.printxpress.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.printxpress.printxpress.R;
import com.printxpress.printxpress.models.Offer;

import java.util.List;

public class OfferAdapter extends RecyclerView.Adapter<OfferAdapter.OfferViewHolder> {

    public interface Listener {
        void onCopyCode(Offer offer);
    }

    private final List<Offer> offers;
    private final Listener listener;

    public OfferAdapter(List<Offer> offers, Listener listener) {
        this.offers = offers;
        this.listener = listener;
    }

    @NonNull
    @Override
    public OfferViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_offer_card, parent, false);
        return new OfferViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull OfferViewHolder h, int position) {
        final Offer offer = offers.get(position);
        Context ctx = h.itemView.getContext();

        h.title.setText(offer.getTitle());
        h.desc.setText(offer.getDescription());
        h.image.setImageResource(offer.getImageRes());
        h.code.setText(offer.getCode());

        boolean active = offer.isActive();
        h.validity.setText(active
                ? ctx.getString(R.string.offer_valid_till, offer.getFormattedValidUntil())
                : ctx.getString(R.string.offer_expired));
        h.copy.setEnabled(active);
        h.copy.setOnClickListener(v -> listener.onCopyCode(offer));
        h.itemView.setAlpha(active ? 1f : 0.55f);
    }

    @Override
    public int getItemCount() {
        return offers.size();
    }

    static class OfferViewHolder extends RecyclerView.ViewHolder {
        final TextView title, desc, validity, code;
        final ImageView image;
        final View copy;

        OfferViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tv_offer_title);
            desc = itemView.findViewById(R.id.tv_offer_description);
            validity = itemView.findViewById(R.id.tv_offer_validity);
            code = itemView.findViewById(R.id.tv_offer_code);
            image = itemView.findViewById(R.id.iv_offer_mockup);
            copy = itemView.findViewById(R.id.btn_offer_copy);
        }
    }
}
