package com.sportmate.app.domain.model;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PlayRequestPage {
    private final List<PlayRequest> items;
    private final int page;
    private final int pageSize;
    private final int totalCount;

    public PlayRequestPage(@NonNull List<PlayRequest> items, int page, int pageSize, int totalCount) {
        this.items = Collections.unmodifiableList(new ArrayList<>(items));
        this.page = page;
        this.pageSize = pageSize;
        this.totalCount = totalCount;
    }

    @NonNull public List<PlayRequest> getItems() { return items; }
    public int getPage() { return page; }
    public int getPageSize() { return pageSize; }
    public int getTotalCount() { return totalCount; }
    public boolean hasNextPage() { return page * pageSize < totalCount; }
}
