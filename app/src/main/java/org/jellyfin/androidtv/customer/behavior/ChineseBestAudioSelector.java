package org.jellyfin.androidtv.customer.behavior;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;

import org.jellyfin.androidtv.customer.common.CustomerCommonUtils;
import org.jellyfin.sdk.model.api.MediaStream;
import org.jellyfin.sdk.model.api.MediaStreamType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timber.log.Timber;

/**
 * 国语选择器
 */
public class ChineseBestAudioSelector implements BestStreamSelector {
    public static class MatchRule {
        private String matchRule;
        private int sort;
        private Pattern pattern;

        public String getMatchRule() {
            return matchRule;
        }

        public void setMatchRule(String matchRule) {
            this.matchRule = matchRule;
            this.pattern = Pattern.compile(matchRule);
        }

        public int getSort() {
            return sort;
        }

        public void setSort(int sort) {
            this.sort = sort;
        }

        public MatchRule() {
        }

        public MatchRule(String matchRule, int sort) {
            setMatchRule(matchRule);
            setSort(sort);
        }

        public boolean match(String content) {
            if (content == null || content.isEmpty()) {
                return false;
            }

            if (pattern == null) {
                this.pattern = Pattern.compile(matchRule);
            }

            String lowerCase = content.toLowerCase();
            Matcher matcher = pattern.matcher(lowerCase);
            if (matcher.matches()) {
                return true;
            }
            return lowerCase.contains(matchRule);
        }
    }
    protected static final int DEFAULT_SORT = 10000;
    protected List<MatchRule> bestMatchSort;
    {
        bestMatchSort = new ArrayList<>();
        bestMatchSort.add(new MatchRule(".*?(dolby|杜比).*", 1000));
        bestMatchSort.add(new MatchRule("环绕音", 100));
        bestMatchSort.add(new MatchRule(".*?(国语|普通话|mandarin).*", -1000));
        bestMatchSort.add(new MatchRule("中文", -100));
        bestMatchSort.add(new MatchRule("粤语", -10));
    }

    private void getBestMatchSortFromHttp() {
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(URL);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setConnectTimeout(10000); // 10秒超时
            urlConnection.setReadTimeout(10000);

            // 获取响应码
            int responseCode = urlConnection.getResponseCode();
            Timber.i("请求数据完成=%s, responseCode=%s", URL, responseCode);
            initSuccess = true;
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // 读取响应内容
                String result = readStream(urlConnection.getInputStream());
                Timber.i("请求数据完成=%s, responseCode=%s, result=%s", URL, responseCode, result);
                ArrayList<MatchRule> matchRules = JSON.parseObject(result, new TypeReference<ArrayList<MatchRule>>() {
                });

                if (matchRules != null) {
                    bestMatchSort = matchRules;
                }
            } else if (responseCode >= 500 && responseCode < 600){
                retryTimes ++;
                initSuccess = false;
            }
        } catch (Exception e) {
            Timber.e(e, "请求数据异常, 重试次数 retryTimes=%s", retryTimes);
            retryTimes ++;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        if (!initSuccess && retryTimes < 3) {
            CustomerCommonUtils
                    .getMainHandler()
                    .postDelayed(() -> new Thread(this::getBestMatchSortFromHttp).start(), 10_1000L);
        }
    }

    private static final String URL = "https://gitee.com/fengymi/gfwlist/raw/jellyfin_best_match_audio/BestMatchAudio.txt";
    private int retryTimes;
    private static boolean initSuccess = false;

    // 读取输入流并转换为字符串
    private String readStream(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder sb = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    public ChineseBestAudioSelector() {
        CustomerCommonUtils.getMainHandler().post(() -> new Thread(this::getBestMatchSortFromHttp).start());
    }

    @Override
    public Integer getBestMatchStream(List<MediaStream> mediaStreams) {
        if (mediaStreams == null || mediaStreams.isEmpty()) {
            return null;
        }

        MediaStream bastMatchMediaStream = mediaStreams
                .stream()
                .filter(mediaStream -> MediaStreamType.AUDIO.equals(mediaStream.getType()))
                .sorted((a, b) -> {
                    int aLevel = getBestSortLevel(Objects.nonNull(a.getDisplayTitle()) ? a.getDisplayTitle() : a.getTitle());
                    int bLevel = getBestSortLevel(Objects.nonNull(b.getDisplayTitle()) ? b.getDisplayTitle() : b.getTitle());
                    return Integer.compare(aLevel, bLevel);
                })
                .findFirst()
                .orElse(null);

        if (bastMatchMediaStream == null) {
            return null;
        }

        int i = mediaStreams.indexOf(bastMatchMediaStream);
        Timber.d("getBestMatchStream %s %d", bastMatchMediaStream.getDisplayTitle(),  i);
        return i;
    }

    protected int getBestSortLevel(String title) {
        if (title == null || title.isEmpty()) {
            return DEFAULT_SORT;
        }

        int sort = DEFAULT_SORT;
        for (MatchRule matchRule : bestMatchSort) {
            if (matchRule.match(title)) {
                sort += matchRule.sort;
            }
        }

//        Timber.d("getBestSortLevel %s %d", title,  sort);
        return sort;
    }
}
