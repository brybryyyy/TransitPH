package com.transitph.app.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.transitph.app.R;
import com.transitph.app.activities.RouteResultsActivity;
import com.transitph.app.database.TerminalDao;
import com.transitph.app.models.Terminal;
import com.transitph.app.models.Destination;

import java.util.ArrayList;
import java.util.List;

public class DestinationAdapter extends RecyclerView.Adapter<DestinationAdapter.ViewHolder> {
    private final Context context;
    private List<Destination> destinationList = new ArrayList<>();

    public DestinationAdapter(Context context) {
        this.context = context;
    }

    public void setDestinations(List<Destination> list) {
        this.destinationList = list != null ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_destination, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Destination item = destinationList.get(position);
        holder.tvName.setText(item.getName());
        holder.tvLocation.setText(item.getMunicipality() + ", " + item.getProvince());
        holder.tvCategory.setText(item.getCategory());
        holder.tvDesc.setText(item.getDescription());
        holder.tvHours.setText("🕒 " + item.getOperatingHours());
        holder.tvTerminal.setText("📍 Near " + item.getNearbyTerminalName());
        holder.tvRoute.setText("Suggested Commute: " + item.getSuggestedRoute());

        holder.btnNavigate.setOnClickListener(v -> {
            Intent intent = new Intent(context, RouteResultsActivity.class);
            String municipality = item.getMunicipality() == null ? "" : item.getMunicipality().trim();
            intent.putExtra(RouteResultsActivity.EXTRA_FROM, "");
            intent.putExtra(RouteResultsActivity.EXTRA_TO, municipality);
            intent.putExtra(RouteResultsActivity.EXTRA_DESTINATION_LABEL, item.getName());

            // If no route reaches the municipality itself, show routes to the nearest saved terminal.
            Terminal nearby = null;
            for (Terminal terminal : new TerminalDao(context).searchTerminals(item.getNearbyTerminalName())) {
                if (terminal.getTerminalName() != null && terminal.getTerminalName().equalsIgnoreCase(item.getNearbyTerminalName())) {
                    nearby = terminal;
                    break;
                }
            }
            if (nearby != null && nearby.getCity() != null && !nearby.getCity().trim().isEmpty()
                    && !nearby.getCity().equalsIgnoreCase(municipality)) {
                intent.putExtra(RouteResultsActivity.EXTRA_DESTINATION_FALLBACK, nearby.getCity());
            }
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return destinationList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvLocation, tvCategory, tvDesc, tvHours, tvTerminal, tvRoute;
        Button btnNavigate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_dest_name);
            tvLocation = itemView.findViewById(R.id.tv_dest_location);
            tvCategory = itemView.findViewById(R.id.tv_dest_category);
            tvDesc = itemView.findViewById(R.id.tv_dest_desc);
            tvHours = itemView.findViewById(R.id.tv_dest_hours);
            tvTerminal = itemView.findViewById(R.id.tv_dest_terminal);
            tvRoute = itemView.findViewById(R.id.tv_dest_route);
            btnNavigate = itemView.findViewById(R.id.btn_navigate_destination);
        }
    }
}
