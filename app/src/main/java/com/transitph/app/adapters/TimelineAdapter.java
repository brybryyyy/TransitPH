package com.transitph.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.transitph.app.R;
import com.transitph.app.services.RouteSearchService.TimelineStep;
import java.util.ArrayList;
import java.util.List;

public class TimelineAdapter extends RecyclerView.Adapter<TimelineAdapter.TimelineViewHolder> {

    private List<TimelineStep> steps = new ArrayList<>();
    private String currentLanguage = "en";

    public TimelineAdapter(List<TimelineStep> steps, String language) {
        this.steps = steps != null ? steps : new ArrayList<>();
        this.currentLanguage = language;
    }

    public void updateSteps(List<TimelineStep> newSteps, String language) {
        this.steps = newSteps != null ? newSteps : new ArrayList<>();
        this.currentLanguage = language;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TimelineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_timeline_step, parent, false);
        return new TimelineViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TimelineViewHolder holder, int position) {
        TimelineStep step = steps.get(position);
        boolean isLast = (position == steps.size() - 1);
        holder.bind(step, currentLanguage, isLast);
    }

    @Override
    public int getItemCount() {
        return steps.size();
    }

    static class TimelineViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvStepBadge;
        private final TextView tvStepIcon;
        private final TextView tvStepTitle;
        private final TextView tvStepInstruction;
        private final TextView tvStepMeta;
        private final View lineConnector;

        public TimelineViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStepBadge = itemView.findViewById(R.id.tv_step_badge);
            tvStepIcon = itemView.findViewById(R.id.tv_step_icon);
            tvStepTitle = itemView.findViewById(R.id.tv_step_title);
            tvStepInstruction = itemView.findViewById(R.id.tv_step_instruction);
            tvStepMeta = itemView.findViewById(R.id.tv_step_meta);
            lineConnector = itemView.findViewById(R.id.line_connector);
        }

        public void bind(TimelineStep step, String lang, boolean isLast) {
            tvStepBadge.setText("Step " + step.getStepNumber());

            String icon = "📍";
            if ("WALK".equalsIgnoreCase(step.getIconType())) icon = "🚶";
            else if ("RIDE_JEEP".equalsIgnoreCase(step.getIconType())) icon = "🚐";
            else if ("RIDE_BUS".equalsIgnoreCase(step.getIconType())) icon = "🚌";
            else if ("ARRIVE".equalsIgnoreCase(step.getIconType())) icon = "🎯";

            tvStepIcon.setText(icon);
            tvStepTitle.setText(step.getTitle(lang));
            tvStepInstruction.setText(step.getInstruction(lang));
            tvStepMeta.setText(step.getTimeOrDistance());

            if (lineConnector != null) {
                lineConnector.setVisibility(isLast ? View.INVISIBLE : View.VISIBLE);
            }
        }
    }
}
