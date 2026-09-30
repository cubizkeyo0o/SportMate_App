package com.sportmate.app.presentation.booking;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.sportmate.app.R;
import com.sportmate.app.databinding.ItemBookingBinding;
import com.sportmate.app.databinding.ItemBookingHeaderBinding;
import com.sportmate.app.domain.model.Booking;
import com.sportmate.app.presentation.common.MoneyFormats;

import java.text.DateFormat;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

/** Section headers and booking rows; tapping a booking opens it. */
final class MyBookingsAdapter extends ListAdapter<BookingGroups.Row, RecyclerView.ViewHolder> {
    interface Listener {
        void onBookingSelected(Booking booking);
    }

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_BOOKING = 1;

    private static final DiffUtil.ItemCallback<BookingGroups.Row> DIFF = new DiffUtil.ItemCallback<BookingGroups.Row>() {
        @Override
        public boolean areItemsTheSame(@NonNull BookingGroups.Row oldItem, @NonNull BookingGroups.Row newItem) {
            if (oldItem.isHeader() || newItem.isHeader()) {
                return oldItem.header == newItem.header;
            }
            return oldItem.booking.getId().equals(newItem.booking.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull BookingGroups.Row oldItem, @NonNull BookingGroups.Row newItem) {
            if (oldItem.isHeader() || newItem.isHeader()) {
                return oldItem.header == newItem.header;
            }
            return oldItem.booking.getStatus().equals(newItem.booking.getStatus())
                    && oldItem.booking.getTotalPrice().compareTo(newItem.booking.getTotalPrice()) == 0;
        }
    };

    private final Listener listener;
    private final Locale locale;
    private Map<String, String> courtNames = Collections.emptyMap();

    MyBookingsAdapter(Listener listener, Locale locale) {
        super(DIFF);
        this.listener = listener;
        this.locale = locale;
    }

    void setCourtNames(Map<String, String> names) {
        courtNames = names == null ? Collections.emptyMap() : names;
        notifyItemRangeChanged(0, getItemCount());
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).isHeader() ? TYPE_HEADER : TYPE_BOOKING;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return viewType == TYPE_HEADER
                ? new HeaderHolder(ItemBookingHeaderBinding.inflate(inflater, parent, false))
                : new BookingHolder(ItemBookingBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        BookingGroups.Row row = getItem(position);
        if (holder instanceof HeaderHolder) {
            ((HeaderHolder) holder).binding.header.setText(row.header);
            return;
        }
        Booking booking = row.booking;
        ItemBookingBinding binding = ((BookingHolder) holder).binding;
        String name = courtNames.get(booking.getCourtId());
        binding.courtName.setText(name != null ? name
                : binding.getRoot().getContext().getString(R.string.booking_court_unlisted));
        binding.sportName.setText(booking.getSport().getName());
        binding.when.setText(binding.getRoot().getContext().getString(R.string.booking_when,
                BookingFormats.date(booking.getLocalDate(), DateFormat.MEDIUM, locale),
                booking.getLocalStartTime(), booking.getLocalEndTime()));
        binding.status.setText(BookingFormats.statusLabel(booking.getStatus()));
        binding.price.setText(binding.getRoot().getContext().getString(R.string.booking_total_value,
                MoneyFormats.amount(booking.getTotalPrice(), booking.getCurrency(), locale), booking.getCurrency()));
        binding.getRoot().setOnClickListener(v -> listener.onBookingSelected(booking));
    }

    static final class HeaderHolder extends RecyclerView.ViewHolder {
        final ItemBookingHeaderBinding binding;

        HeaderHolder(ItemBookingHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    static final class BookingHolder extends RecyclerView.ViewHolder {
        final ItemBookingBinding binding;

        BookingHolder(ItemBookingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
