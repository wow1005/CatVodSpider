package com.github.catvod.spider;

import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.github.catvod.bean.Class;
import com.github.catvod.bean.Result;
import com.github.catvod.bean.Vod;
import com.github.catvod.crawler.Spider;
import com.github.catvod.crawler.SpiderDebug;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CustomSearchButton extends Spider {

    private static final int TARGET_CONTAINER_INDEX = 6;
    private boolean buttonInjected = false;
    private List<ViewGroup> candidateContainers = new ArrayList<>();

    @Override
    public void init(Context context, String extend) throws Exception {
        super.init(context, extend);
        Init.init(context);
        SpiderDebug.log("CustomSearchButton: 初始化");

        if (context instanceof Application) {
            ((Application) context).registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
                        @Override
                        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}

                        @Override
                        public void onActivityStarted(Activity activity) {}

                        @Override
                        public void onActivityResumed(Activity activity) {
                            String activityName = activity.getClass().getSimpleName();
                            if ("VideoActivity".equals(activityName)) {
                                SpiderDebug.log("CustomSearchButton: 检测到VideoActivity");
                                Init.post(() -> injectButton(), 1000);
                            }
                        }

                        @Override
                        public void onActivityPaused(Activity activity) {
                            if ("VideoActivity".equals(activity.getClass().getSimpleName())) {
                                buttonInjected = false;
                            }
                        }

                        @Override
                        public void onActivityStopped(Activity activity) {}

                        @Override
                        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

                        @Override
                        public void onActivityDestroyed(Activity activity) {}
                    });
        }
    }

    private void injectButton() {
        Activity activity = Init.activity();
        if (activity == null || buttonInjected) return;

        try {
            View decorView = activity.getWindow().getDecorView();
            candidateContainers.clear();
            findAllVerticalLinearLayouts(decorView);

            if (candidateContainers.size() <= TARGET_CONTAINER_INDEX) {
                SpiderDebug.log("CustomSearchButton: 容器数量不足,找到" + candidateContainers.size() + "个");
                return;
            }

            ViewGroup targetContainer = candidateContainers.get(TARGET_CONTAINER_INDEX);
            SpiderDebug.log("CustomSearchButton: 使用容器#" + TARGET_CONTAINER_INDEX);

            // 强制设置容器可见
            targetContainer.setVisibility(View.VISIBLE);
            ViewGroup.LayoutParams containerParams = targetContainer.getLayoutParams();
            if (containerParams != null) {
                if (containerParams.width == 0)
                    containerParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                if (containerParams.height == 0)
                    containerParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                targetContainer.setLayoutParams(containerParams);
            }

            // 创建并添加按钮
            ImageView button = createSearchButton(activity);
            targetContainer.addView(button);
            targetContainer.requestLayout();

            buttonInjected = true;
            SpiderDebug.log("CustomSearchButton: 按钮添加成功");
            showToast(activity, "搜索按钮已添加");

        } catch (Throwable e) {
            SpiderDebug.log("CustomSearchButton: 注入失败 - " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void findAllVerticalLinearLayouts(View parent) {
        if (parent instanceof LinearLayout) {
            LinearLayout ll = (LinearLayout) parent;
            if (ll.getOrientation() == LinearLayout.VERTICAL) {
                candidateContainers.add(ll);
            }
        }

        if (parent instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) parent;
            for (int i = 0; i < group.getChildCount(); i++) {
                findAllVerticalLinearLayouts(group.getChildAt(i));
            }
        }
    }

    private ImageView createSearchButton(Context context) {
        ImageView button = new ImageView(context);

        // 设置与danmuSetting相同的大小
        int size = dp(context, 40);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
        params.setMarginStart(dp(context, 8));
        params.topMargin = dp(context, 8);
        button.setLayoutParams(params);

        // 设置可见性
        button.setVisibility(View.VISIBLE);

        // 设置背景(与danmuSetting一致)
        try {
            button.setBackgroundResource(android.R.attr.selectableItemBackgroundBorderless);
        } catch (Exception e) {
            button.setBackgroundColor(0x00000000);
        }

        // 设置padding
        int padding = dp(context, 8);
        button.setPadding(padding, padding, padding, padding);

        // 设置搜索图标
        try {
            int iconId = context.getResources().getIdentifier("ic_action_search", "drawable", context.getPackageName());
            if (iconId != 0) {
                button.setImageResource(iconId);
            } else {
                // 如果找不到搜索图标,使用简单的背景色作为替代
                button.setBackgroundColor(0xFF2196F3);
            }
        } catch (Exception e) {
            button.setBackgroundColor(0xFF2196F3);
        }

        // 设置点击事件
        button.setClickable(true);
        button.setFocusable(true);
        button.setOnClickListener(v -> {
            SpiderDebug.log("CustomSearchButton: 搜索按钮被点击");
            showBottomDialog(context);
        });

        SpiderDebug.log("CustomSearchButton: 搜索按钮创建完成");
        return button;
    }

    private void showBottomDialog(Context context) {
        Activity activity = (Activity) Init.activity();
        if (activity == null) return;

        try {
            // 获取屏幕尺寸
            int screenWidth = getScreenWidth(context);
            int screenHeight = getScreenHeight(context);
            int dialogHeight = screenHeight / 2;

            SpiderDebug.log("CustomSearchButton: 屏幕尺寸=" + screenWidth + "x" + screenHeight);
            SpiderDebug.log("CustomSearchButton: 对话框尺寸=" + screenWidth + "x" + dialogHeight);

            // 创建对话框
            Dialog dialog = new Dialog(activity);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

            // 创建根布局
            LinearLayout rootLayout = new LinearLayout(activity);
            rootLayout.setOrientation(LinearLayout.VERTICAL);
            rootLayout.setBackgroundColor(Color.WHITE);
            rootLayout.setPadding(dp(activity, 16), dp(activity, 16), dp(activity, 16), dp(activity, 16));

            // 创建搜索框容器
            LinearLayout searchContainer = new LinearLayout(activity);
            searchContainer.setOrientation(LinearLayout.HORIZONTAL);
            searchContainer.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
            );
            searchContainer.setLayoutParams(searchParams);

            // 创建搜索输入框
            EditText searchInput = new EditText(activity);
            LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1.0f
            );
            searchInput.setLayoutParams(inputParams);
            searchInput.setHint("请输入搜索关键词");
            searchInput.setTextSize(16);
            searchInput.setTextColor(0xFF333333);
            searchInput.setPadding(dp(activity, 12), dp(activity, 12), dp(activity, 12), dp(activity, 12));
            searchInput.setBackgroundColor(0xFFF5F5F5);

            // 创建搜索按钮
            TextView searchButton = new TextView(activity);
            LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
            );
            buttonParams.setMarginStart(dp(activity, 8));
            searchButton.setLayoutParams(buttonParams);
            searchButton.setText("搜索");
            searchButton.setTextSize(16);
            searchButton.setTextColor(Color.WHITE);
            searchButton.setBackgroundColor(0xFF2196F3);
            searchButton.setPadding(dp(activity, 16), dp(activity, 12), dp(activity, 16), dp(activity, 12));

            searchContainer.addView(searchInput);
            searchContainer.addView(searchButton);

            // 创建可滚动区域
            ScrollView scrollView = new ScrollView(activity);
            LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1.0f
            );
            scrollParams.topMargin = dp(activity, 16);
            scrollView.setLayoutParams(scrollParams);

            // 创建结果容器
            LinearLayout resultContainer = new LinearLayout(activity);
            resultContainer.setOrientation(LinearLayout.VERTICAL);
            resultContainer.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            scrollView.addView(resultContainer);

            // 搜索按钮点击事件
            searchButton.setOnClickListener(v -> {
                String keyword = searchInput.getText().toString().trim();
                if (!keyword.isEmpty()) {
                    performSearch(activity, resultContainer, keyword);
                }
            });

            // 组装布局
            rootLayout.addView(searchContainer);
            rootLayout.addView(scrollView);

            // 设置对话框内容
            dialog.setContentView(rootLayout);

            // 设置对话框窗口属性
            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                window.setGravity(Gravity.BOTTOM);

                WindowManager.LayoutParams params = window.getAttributes();
                params.width = screenWidth;
                params.height = dialogHeight;
                window.setAttributes(params);

                SpiderDebug.log("CustomSearchButton: 对话框窗口属性设置完成");
            }

            dialog.show();
            SpiderDebug.log("CustomSearchButton: 对话框显示成功");

        } catch (Throwable e) {
            SpiderDebug.log("CustomSearchButton: 对话框显示失败 - " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void performSearch(Context context, LinearLayout resultContainer, String keyword) {
        resultContainer.removeAllViews();

        TextView loadingText = new TextView(context);
        loadingText.setText("搜索: " + keyword);
        loadingText.setTextSize(16);
        loadingText.setTextColor(0xFF333333);
        loadingText.setPadding(0, dp(context, 8), 0, dp(context, 8));
        resultContainer.addView(loadingText);

        SpiderDebug.log("CustomSearchButton: 执行搜索 - " + keyword);
    }

    private int getScreenWidth(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        return wm.getDefaultDisplay().getWidth();
    }

    private int getScreenHeight(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        return wm.getDefaultDisplay().getHeight();
    }

    private void showToast(Context context, String message) {
        Init.post(() -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
    }

    private int dp(Context context, int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

    @Override
    public String homeContent(boolean filter) {
        return Result.string(new ArrayList<>(), new ArrayList<>());
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<
                    String, String> extend) {
        return Result.string(new ArrayList<>());
    }

    @Override
    public String detailContent(List<String> ids) {
        Vod vod = new Vod();
        vod.setVodId(ids.get(0));
        return Result.string(vod);
    }

    @Override
    public String searchContent(String key, boolean quick) {
        return Result.string(new ArrayList<>());
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) {
        return Result.get().url(id).string();
    }
}
