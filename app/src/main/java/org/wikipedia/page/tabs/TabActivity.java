package org.wikipedia.page.tabs;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.BaseActivity;
import org.wikipedia.page.PageTitle;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.util.log.L;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.mrapp.android.tabswitcher.Animation;
import de.mrapp.android.tabswitcher.Tab;
import de.mrapp.android.tabswitcher.TabSwitcher;
import de.mrapp.android.tabswitcher.TabSwitcherDecorator;
import de.mrapp.android.tabswitcher.TabSwitcherListener;
import de.mrapp.android.util.logging.LogLevel;

public class TabActivity extends BaseActivity {
    public static final int RESULT_LOAD_FROM_BACKSTACK = 10;
    public static final int RESULT_NEW_TAB = 11;

    private static final int MAX_CACHED_BMP_SIZE = 800;

    @BindView(R.id.tab_switcher) TabSwitcher tabSwitcher;
    @BindView(R.id.tab_toolbar) Toolbar tabToolbar;
    private WikipediaApp app;
    private TabListener tabListener = new TabListener();

    @Nullable private static Bitmap FIRST_TAB_BITMAP;

    public static void captureFirstTabBitmap(@NonNull View view) {
        clearFirstTabBitmap();
        Bitmap bmp = null;
        try {
            boolean wasCacheEnabled = view.isDrawingCacheEnabled();
            if (!wasCacheEnabled) {
                view.setDrawingCacheEnabled(true);
                view.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);
            }
            Bitmap cacheBmp = view.getDrawingCache();
            if (cacheBmp != null) {
                int width;
                int height;
                if (cacheBmp.getWidth() > cacheBmp.getHeight()) {
                    width = MAX_CACHED_BMP_SIZE;
                    height = width * cacheBmp.getHeight() / cacheBmp.getWidth();
                } else {
                    height = MAX_CACHED_BMP_SIZE;
                    width = height * cacheBmp.getWidth() / cacheBmp.getHeight();
                }
                bmp = Bitmap.createScaledBitmap(cacheBmp, width, height, true);
            }
            if (!wasCacheEnabled) {
                view.setDrawingCacheEnabled(false);
            }
        } catch (OutOfMemoryError e) {
            // don't worry about it
        }
        FIRST_TAB_BITMAP = bmp;
    }

    private static void clearFirstTabBitmap() {
        if (FIRST_TAB_BITMAP != null) {
            if (!FIRST_TAB_BITMAP.isRecycled()) {
                FIRST_TAB_BITMAP.recycle();
            }
            FIRST_TAB_BITMAP = null;
        }
    }

    public static Intent newIntent(@NonNull Context context) {
        return new Intent(context, TabActivity.class);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tabs);
        ButterKnife.bind(this);
        app = WikipediaApp.getInstance();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setStatusBarColor(R.color.base20);
        }
        setSupportActionBar(tabToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("");

        tabSwitcher.setDecorator(new TabSwitcherDecorator() {
            @NonNull
            @Override
            public View onInflateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent, int viewType) {
                if (viewType == 1) {
                    ImageView view = new AppCompatImageView(TabActivity.this);
                    view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                    view.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    view.setImageBitmap(FIRST_TAB_BITMAP);
                    return view;
                }
                return inflater.inflate(R.layout.item_tab_contents, parent, false);
            }

            @Override
            public void onShowTab(@NonNull Context context, @NonNull TabSwitcher tabSwitcher, @NonNull View view,
                                  @NonNull Tab tab, int index, int viewType, @Nullable Bundle savedInstanceState) {
                int tabIndex = app.getTabList().size() - index - 1;
                if (viewType == 1) {
                    return;
                }
                TextView titleText = view.findViewById(R.id.tab_article_title);
                TextView descriptionText = view.findViewById(R.id.tab_article_description);

                PageTitle title = app.getTabList().get(tabIndex).getTopMostTitle();
                titleText.setText(title.getDisplayText());
                if (TextUtils.isEmpty(title.getDescription())) {
                    descriptionText.setVisibility(View.GONE);
                } else {
                    descriptionText.setText(title.getDescription());
                    descriptionText.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public int getViewType(@NonNull final Tab tab, final int index) {
                if (index == 0 && FIRST_TAB_BITMAP != null && !FIRST_TAB_BITMAP.isRecycled()) {
                    return 1;
                }
                return 0;
            }

            @Override
            public int getViewTypeCount() {
                return 2;
            }
        });

        for (int i = 0; i < app.getTabList().size(); i++) {
            int tabIndex = app.getTabList().size() - i - 1;
            if (app.getTabList().get(tabIndex).getBackStack().isEmpty()) {
                continue;
            }
            Tab tab = new Tab(app.getTabList().get(tabIndex).getTopMostTitle().getDisplayText());
            tab.setIcon(R.drawable.ic_image_black_24dp);
            tab.setIconTint(ResourceUtil.getThemedColor(this, R.attr.material_theme_secondary_color));
            tab.setTitleTextColor(ResourceUtil.getThemedColor(this, R.attr.material_theme_secondary_color));
            tab.setCloseButtonIcon(R.drawable.ic_close_white_24dp);
            tab.setCloseButtonIconTint(ResourceUtil.getThemedColor(this, R.attr.material_theme_secondary_color));
            tab.setCloseable(true);
            tab.setParameters(new Bundle());
            tabSwitcher.addTab(tab);
        }

        tabSwitcher.setLogLevel(LogLevel.OFF);
        tabSwitcher.addListener(tabListener);
        tabSwitcher.showSwitcher();
    }

    @Override
    public void onDestroy() {
        tabSwitcher.removeListener(tabListener);
        clearFirstTabBitmap();
        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();
        app.commitTabState();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_tabs, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        ResourceUtil.setMenuItemTint(menu.findItem(R.id.menu_close_all_tabs), Color.WHITE);
        ResourceUtil.setMenuItemTint(menu.findItem(R.id.menu_new_tab), Color.WHITE);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_close_all_tabs:
                AlertDialog.Builder alert = new AlertDialog.Builder(this);
                alert.setMessage(R.string.close_all_tabs_confirm);
                alert.setPositiveButton(R.string.close_all_tabs_confirm_yes, (dialog, which) -> {
                    app.getTabList().clear();
                    setResult(RESULT_LOAD_FROM_BACKSTACK);
                    finish();
                });
                alert.setNegativeButton(R.string.close_all_tabs_confirm_no, null);
                alert.create().show();
                return true;
            case R.id.menu_new_tab:
                setResult(RESULT_NEW_TAB);
                finish();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private class TabListener implements TabSwitcherListener {
        @Override public void onSwitcherShown(@NonNull TabSwitcher tabSwitcher) { }

        @Override public void onSwitcherHidden(@NonNull TabSwitcher tabSwitcher) { }

        @Override
        public void onSelectionChanged(@NonNull TabSwitcher tabSwitcher, int index, @Nullable Tab selectedTab) {
            int tabIndex = app.getTabList().size() - index - 1;
            L.d("Tab selected: " + index);

            if (tabIndex < app.getTabList().size() - 1) {
                org.wikipedia.page.tabs.Tab tab = app.getTabList().remove(tabIndex);
                app.getTabList().add(tab);
            }
            setResult(RESULT_LOAD_FROM_BACKSTACK);
            finish();
        }

        @Override
        public void onTabAdded(@NonNull TabSwitcher tabSwitcher, int index, @NonNull Tab tab, @NonNull Animation animation) { }

        @Override
        public void onTabRemoved(@NonNull TabSwitcher tabSwitcher, int index, @NonNull Tab tab, @NonNull Animation animation) {
            int tabIndex = app.getTabList().size() - index - 1;

            app.getTabList().remove(tabIndex);
            //tabFunnel.logClose(app.getTabList().size(), index);
            if (app.getTabList().size() == 0) {
                //tabFunnel.logCancel(app.getTabList().size());

            } else if (index == app.getTabList().size()) {
                // if it's the topmost tab, then load the topmost page in the next tab.

            }
            setResult(RESULT_LOAD_FROM_BACKSTACK);
        }

        @Override
        public void onAllTabsRemoved(@NonNull TabSwitcher tabSwitcher, @NonNull Tab[] tabs, @NonNull Animation animation) {
            L.d("All tabs removed.");
        }
    }
}
