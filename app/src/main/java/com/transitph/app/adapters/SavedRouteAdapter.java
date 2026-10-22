package com.transitph.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.transitph.app.R;
import com.transitph.app.models.SavedRoute;
import com.transitph.app.utils.RouteFarePreference;
import java.util.ArrayList;
import java.util.List;

public class SavedRouteAdapter extends RecyclerView.Adapter<SavedRouteAdapter.SavedRouteViewHolder> {

    public interface OnSavedRouteClickListener {
        void onViewRoute(SavedRoute savedRoute);
        void onDeleteRoute(SavedRoute savedRoute);
    }

    private List<SavedRoute> savedRoutes = new ArrayList<>();
    private final OnSavedRouteClickListener listener;

    public SavedRouteAdapter(List<SavedRoute> savedRoutes, OnSavedRouteClickListener listener) {
        this.savedRoutes = savedRoutes != null ? savedRoutes : new ArrayList<>();
        this.listener = listener;
    }

    public void setSavedRoutes(List<SavedRoute> newSavedRoutes) {
        this.savedRoutes = newSavedRoutes != null ? newSavedRoutes : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SavedRouteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_saved_route, parent, false);
        return new SavedRouteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SavedRouteViewHolder holder, int position) {
        SavedRoute sr = savedRoutes.get(position);
        holder.bind(sr, listener);
    }

    @Override
    public int getItemCount() {
        return savedRoutes.size();
    }

    static class SavedRouteViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvRouteName;
        private final TextView tvFromTo;
        private final TextView tvFare;
        private final TextView tvTravelTime;
        private final TextView tvTerminal;
        private final Button btnView;
        private final Button btnDelete;

        public SavedRouteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRouteName = itemView.findViewById(R.id.tv_saved_route_name);
            tvFromTo = itemView.findViewById(R.id.tv_saved_from_to);
            tvFare = itemView.findViewById(R.id.tv_saved_fare);
            tvTravelTime = itemView.findViewById(R.id.tv_saved_travel_time);
            tvTerminal = itemView.findViewById(R.id.tv_saved_terminal);
            btnView = itemView.findViewById(R.id.btn_view_saved);
            btnDelete = itemView.findViewById(R.id.btn_delete_saved);
        }

        public void bind(SavedRoute sr, OnSavedRouteClickListener listener) {
            tvRouteName.setText(sr.getRouteName());
            tvFromTo.setText(sr.getOrigin() + " → " + sr.getDestination());
            tvFare.setText(RouteFarePreference.formatFare(itemView.getContext(), sr));
            tvTravelTime.setText(sr.getEstimatedTravelTime() + " min");
            tvTerminal.setText("📍 " + (sr.getTerminalName() != null ? sr.getTerminalName() : "Terminal"));

            if (btnView != null) {
                btnView.setOnClickListener(v -> {
                    if (listener != null) listener.onViewRoute(sr);
                });
            }

            if (btnDelete != null) {
                btnDelete.setOnClickListener(v -> {
                    if (listener != null) listener.onDeleteRoute(sr);
                });
            }
        }
    }
}
