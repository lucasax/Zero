package com.lucasasselli.zero.data;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.lucasasselli.zero.BuildConfig;
import com.lucasasselli.zero.utils.InternalData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import static com.lucasasselli.zero.Constants.LD_CATALOG;

public class Catalog extends ArrayList<CatalogItem> {

    public static final int SORT_BY_POPULARITY = 0;
    public static final int SORT_BY_NEW = 1;
    public static final int SORT_BY_TITLE = 2;
    public static final int SORT_BY_AUTHOR = 3;

    private int currentSortMode = SORT_BY_POPULARITY;
    private float popularityFix = 1;

    public void sort(int sortMode) {

        currentSortMode = sortMode;

        switch (sortMode) {
            case SORT_BY_POPULARITY:
                currentSortMode = SORT_BY_POPULARITY;
                Collections.sort(this, comparatorByPopularity);
                break;

            case SORT_BY_NEW:
                currentSortMode = SORT_BY_NEW;
                Collections.sort(this, comparatorByNew);
                break;

            case SORT_BY_AUTHOR:
                currentSortMode = SORT_BY_AUTHOR;
                Collections.sort(this, comparatorByAuthor);
                break;

            case SORT_BY_TITLE:
                currentSortMode = SORT_BY_TITLE;
                Collections.sort(this, comparatorByTitle);
                break;

            default:
                Collections.sort(this, comparatorByPopularity);
                break;
        }
    }

    public boolean loadFromCache(Context context) {

        InternalData internalData = new InternalData(context);

        // Get catalog as JSON
        String content = internalData.readString(LD_CATALOG, "");

        // Parse as array
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        CatalogItem[] items;

        try {
            items = gson.fromJson(content, CatalogItem[].class);
        } catch (JsonSyntaxException ex) {
            return false;
        }

        if (items != null) {
            // Clear catalog only if there is something to load into
            clear();

            if (BuildConfig.DEBUG) {
                // If the APK is compiled for debug load also the test content
                addAll(Arrays.asList(items));
            } else {
                // If the APK is compiled for production remove the test content
                for (CatalogItem item : items) {
                    if (item.getTest() != 1) {
                        add(item);
                    }
                }
            }

            // Sort it
            computePopularityFix();

            sort(currentSortMode);
        } else {
            // Shouldn't happen, just in case
            return false;
        }

        return true;
    }

    // Comparators
    private final Comparator<CatalogItem> comparatorByPopularity = new Comparator<CatalogItem>() {
        @Override
        public int compare(CatalogItem lhs, CatalogItem rhs) {
            Float lhsFloat = (float) lhs.getDownloads();
            Float rhsFloat = (float) rhs.getDownloads();

            if (lhs.isPro() && !rhs.isPro()) rhsFloat *= popularityFix;
            if (!lhs.isPro() && rhs.isPro()) lhsFloat *= popularityFix;

            return rhsFloat.compareTo(lhsFloat);
        }
    };

    private final Comparator<CatalogItem> comparatorByNew = new Comparator<CatalogItem>() {
        @Override
        public int compare(CatalogItem lhs, CatalogItem rhs) {
            return Integer.valueOf(rhs.getId()).compareTo(Integer.valueOf(lhs.getId()));
        }
    };

    private final Comparator<CatalogItem> comparatorByAuthor = new Comparator<CatalogItem>() {
        @Override
        public int compare(CatalogItem lhs, CatalogItem rhs) {
            if (lhs.getAuthor().compareTo(rhs.getAuthor()) != 0) {
                return lhs.getAuthor().compareTo(rhs.getAuthor());
            } else {
                return lhs.getTitle().compareTo(rhs.getTitle());
            }
        }
    };

    private final Comparator<CatalogItem> comparatorByTitle = new Comparator<CatalogItem>() {
        @Override
        public int compare(CatalogItem lhs, CatalogItem rhs) {
            return lhs.getTitle().compareTo(rhs.getTitle());
        }
    };

    // Other
    private void computePopularityFix() {
        float proCount = 0;
        float normCount = 0;
        float proDownloads = 0;
        float normDownloads = 0;

        for (int i = 0; i < size(); i++) {
            CatalogItem item = get(i);
            if (item.isPro()) {
                proCount++;
                proDownloads += item.getDownloads();
            } else {
                normCount++;
                normDownloads += item.getDownloads();
            }
        }

        float proK = proDownloads / proCount;
        float normK = normDownloads / normCount;

        popularityFix = proK / normK;
    }
}
