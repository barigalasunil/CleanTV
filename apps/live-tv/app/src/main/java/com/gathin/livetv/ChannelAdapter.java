package com.gathin.livetv;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The channel list: one header per language, then a card per channel. */
final class ChannelAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    /** What the list does when a card is picked. */
    interface OnChannelClick {
        void onChannel(Channel channel);
    }

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_CHANNEL = 1;

    /** One line of the list: either a header or a channel. */
    private static final class Row {
        final String header;
        final Channel channel;

        Row(String header, Channel channel) {
            this.header = header;
            this.channel = channel;
        }
    }

    private final List<Row> rows = new ArrayList<>();
    private final LogoLoader logos;
    private final OnChannelClick click;

    ChannelAdapter(LogoLoader logos, OnChannelClick click) {
        this.logos = logos;
        this.click = click;
    }

    /** Replaces the list, grouping the channels by language and, inside it, by their group name. */
    void setChannels(List<Channel> channels) {
        rows.clear();
        Map<String, List<Channel>> byLanguage = new LinkedHashMap<>();
        for (Channel channel : channels) {
            List<Channel> list = byLanguage.get(channel.language);
            if (list == null) {
                list = new ArrayList<>();
                byLanguage.put(channel.language, list);
            }
            list.add(channel);
        }

        for (Map.Entry<String, List<Channel>> entry : byLanguage.entrySet()) {
            String language = entry.getKey();
            rows.add(new Row(Languages.label(language), null));
            Map<String, List<Channel>> byGroup = new LinkedHashMap<>();
            for (Channel channel : entry.getValue()) {
                String group = channel.group == null || channel.group.isEmpty()
                        ? "" : channel.group;
                List<Channel> list = byGroup.get(group);
                if (list == null) {
                    list = new ArrayList<>();
                    byGroup.put(group, list);
                }
                list.add(channel);
            }
            for (List<Channel> group : byGroup.values()) {
                for (Channel channel : group) {
                    rows.add(new Row(null, channel));
                }
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).header != null ? TYPE_HEADER : TYPE_CHANNEL;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new HeaderHolder(inflater.inflate(R.layout.item_header, parent, false));
        }
        return new ChannelHolder(inflater.inflate(R.layout.item_channel, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = rows.get(position);
        if (holder instanceof HeaderHolder) {
            ((HeaderHolder) holder).title.setText(row.header);
            return;
        }
        ChannelHolder card = (ChannelHolder) holder;
        Channel channel = row.channel;
        card.name.setText(channel.label());
        card.group.setText(channel.group);
        card.group.setVisibility(channel.group == null || channel.group.isEmpty() ? View.GONE : View.VISIBLE);
        logos.load(card.logo, channel.logo);
        card.itemView.setOnClickListener(v -> {
            int at = holder.getBindingAdapterPosition();
            if (at >= 0 && at < rows.size() && rows.get(at).channel != null) {
                click.onChannel(rows.get(at).channel);
            }
        });
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    /** The language header. */
    static final class HeaderHolder extends RecyclerView.ViewHolder {
        final TextView title;

        HeaderHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.headerTitle);
        }
    }

    /** One channel card. */
    static final class ChannelHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView group;
        final ImageView logo;

        ChannelHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.channelName);
            group = itemView.findViewById(R.id.channelGroup);
            logo = itemView.findViewById(R.id.channelLogo);
        }
    }
}
