package com.transitph.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.transitph.app.R;
import com.transitph.app.models.Route;
import java.util.ArrayList;
import java.util.List;

public class RouteAdapter extends RecyclerView.Adapter<RouteAdapter.RouteViewHolder> {

    public interface OnRouteClickListener {
        void onRouteClick(Route route);
        void onSaveRouteClick(Route route);
    }

    private List<Route> routes = new ArrayList<>();
    private final OnRouteClickListener listener;

    public RouteAdapter(List<Route> routes, OnRouteClickListener listener) {
        this.routes = routes != null ? routes : new ArrayList<>();
        this.listener = listener;
    }

    public void setRoutes(List<Route> newRoutes) {
        this.routes = newRoutes != null ? newRoutes : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RouteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_route, parent, false);
        return new RouteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RouteViewHolder holder, int position) {
        Route route = routes.get(position);
        holder.bind(route, listener);
    }

    @Override
    public int getItemCount() {
        return routes.size();
    }

    static class RouteViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTransportBadge;
        private final TextView tvRouteTitle;
        private final TextView tvTerminalName;
        private final TextView tvFare;
        private final TextView tvTravelTime;
        private final TextView tvTransfers;
        private final TextView tvWalking;
        private final Button btnViewRoute;
        private final Button btnSaveRoute;

        public RouteViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTransportBadge = itemView.findViewById(R.id.tv_transport_badge);
            tvRouteTitle = itemView.findViewById(R.id.tv_route_title);
            tvTerminalName = itemView.findViewById(R.id.tv_terminal_name);
            tvFare = itemView.findViewById(R.id.tv_fare);
            tvTravelTime = itemView.findViewById(R.id.tv_travel_time);
            tvTransfers = itemView.findViewById(R.id.tv_transfers);
            tvWalking = itemView.findViewById(R.id.tv_walking);
            btnViewRoute = itemView.findViewById(R.id.btn_view_route);
            btnSaveRoute = itemView.findViewById(R.id.btn_save_route);
        }

        public void bind(Route route, OnRouteClickListener listener) {
            tvTransportBadge.setText(route.getTransportIconText());
            tvRouteTitle.setText(route.getOrigin() + " → " + route.getDestination());
            tvTerminalName.setText(route.getTerminalName() != null ? "📍 " + route.getTerminalName() : "📍 Terminal");
            tvFare.setText(route.getFormattedFare());
            tvTravelTime.setText(route.getFormattedTime());
            tvTransfers.setText(route.getTransferCount() + " transfer");
            tvWalking.setText("🚶 " + route.getWalkingDistanceMeters() + "m walking");

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onRouteClick(route);
            });

            if (btnViewRoute != null) {
                btnViewRoute.setOnClickListener(v -> {
                    if (listener != null) listener.onRouteClick(route);
                });
            }

            if (btnSaveRoute != null) {
                btnSaveRoute.setOnClickListener(v -> {
                    if (listener != null) listener.onSaveRouteClick(route);
                });
            }
        }

    }
}
