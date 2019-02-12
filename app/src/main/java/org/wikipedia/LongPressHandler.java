package org.wikipedia;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ListView;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.PageTitle;

import static org.wikipedia.util.DeviceUtil.hideSoftKeyboard;
import static org.wikipedia.util.UriUtil.isValidPageLink;

public class LongPressHandler implements View.OnCreateContextMenuListener,
        MenuItem.OnMenuItemClickListener {
    private final ContextMenuListener contextMenuListener;
    private final int historySource;
    @Nullable private final String referrer;

    private PageTitle title;
    private HistoryEntry entry;

    public LongPressHandler(@NonNull View view, int historySource, @Nullable String referrer,
                            @NonNull ContextMenuListener listener) {
        this.historySource = historySource;
        this.contextMenuListener = listener;
        this.referrer = referrer;
        view.setOnCreateContextMenuListener(this);
    }

    public LongPressHandler(@NonNull View view, int historySource,
                            @NonNull ContextMenuListener listener) {
        this(view, historySource, null, listener);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        title = null;
        if (view instanceof WebView) {
            WebView.HitTestResult result = ((WebView) view).getHitTestResult();
            if (result.getType() == WebView.HitTestResult.SRC_ANCHOR_TYPE) {
                Uri uri = Uri.parse(result.getExtra());
                if (isValidPageLink(uri)) {
                    title = ((WebViewContextMenuListener) contextMenuListener).getWikiSite()
                            .titleForInternalLink(uri.getPath());
                }
            }
        } else if (view instanceof ListView) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            title = ((ListViewContextMenuListener) contextMenuListener)
                    .getTitleForListPosition(info.position);
        }

        if (title != null && !title.isSpecial()) {
            hideSoftKeyboard(view);
            entry = new HistoryEntry(title, historySource);
            entry.setReferrer(referrer);
            new MenuInflater(view.getContext()).inflate(R.menu.menu_page_long_press, menu);
            menu.setHeaderTitle(title.getDisplayText());
            for (int i = 0; i < menu.size(); i++) {
                menu.getItem(i).setOnMenuItemClickListener(this);
            }
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_long_press_open_page:
                contextMenuListener.onOpenLink(title, entry);
                return true;
            case R.id.menu_long_press_open_in_new_tab:
                contextMenuListener.onOpenInNewTab(title, entry);
                return true;
            case R.id.menu_long_press_copy_page:
                contextMenuListener.onCopyLink(title);
                return true;
            case R.id.menu_long_press_share_page:
                contextMenuListener.onShareLink(title);
                return true;
            case R.id.menu_long_press_add_to_list:
                contextMenuListener.onAddToList(title,
                        Constants.InvokeSource.CONTEXT_MENU);
                return true;
            default:
            return false;
        }
    }

    public interface ContextMenuListener {
        void onOpenLink(PageTitle title, HistoryEntry entry);
        void onOpenInNewTab(PageTitle title, HistoryEntry entry);
        void onCopyLink(PageTitle title);
        void onShareLink(PageTitle title);
        void onAddToList(PageTitle title, Constants.InvokeSource source);
    }

    public interface ListViewContextMenuListener extends ContextMenuListener {
        PageTitle getTitleForListPosition(int position);
    }

    public interface WebViewContextMenuListener extends ContextMenuListener {
        WikiSite getWikiSite();
    }
}
