package com.vdurmont.generate;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * This app generate the emoji json from <a href="https://unicode.org/emoji/charts/full-emoji-list.html">https://unicode.org/emoji/charts/full-emoji-list.html</a> ;)
 * <p/>
 * Run with:
 * main()
 */
public class JsonGenerator {
    private static final String resourcePath = getProjectPath() + "/src/main/resources";
    // download file by proxy
    private static final String PROXY_HOST = "127.0.0.1";
    private static final String PROXY_PORT = "1080";
    // load offline file replace download file
    private static final String OFFLINE_PATH = "";
    private static final String ONLINE_URL = "https://unicode.org/emoji/charts/full-emoji-list.html";

    public static void main(String[] args) throws IOException {
        String emojiJsonPath = resourcePath + "/emojis.json";
        String emojiI18NJsonPath = resourcePath + "/emojis.i18n.json";

        Document root = getDocument();

        Elements tdTags, trTags = root.getElementsByTag("tr");
        String aliasBigHead = null, aliasMediumHead = null;
        JSONArray emojis = new JSONArray();
        Map<String, String> emojiI18nMap = getI18nMapFromEmojiI18nJson(emojiI18NJsonPath);
        for (Element trTag : trTags) {
            Element bighead = trTag.select("th.bighead>a").first();
            if (!Objects.isNull(bighead)) {
                aliasBigHead = bighead.attr("name");
                continue;
            }
            Element mediumhead = trTag.select("th.mediumhead>a").first();
            if (!Objects.isNull(mediumhead)) {
                aliasMediumHead = mediumhead.attr("name");
                continue;
            }
            tdTags = trTag.children();
            if (!tdTags.get(1).hasClass("code")) {
                continue;
            }
            String desc = tdTags.last().text().replaceAll("[^\\p{L}\\p{M}\\p{N}\\p{P}\\p{Z}\\p{Cf}\\p{Cs}\\p{Sc}\\s]", "");

            String emojiChar = tdTags.get(2).text();
			JSONObject emoji = new JSONObject();
            emoji.put("emojiChar", emojiChar);
            emoji.put("description", emojiI18nMap.getOrDefault(emojiChar, desc));
            emoji.put("emoji", convertEmoji2Unicode(emojiChar));
            emoji.put("aliases", Collections.singletonList(desc.replace(" ", "_")));
            emoji.put("tags", Arrays.asList(aliasBigHead, aliasMediumHead));
            emojis.put(emoji);
        }
        // json toString无法输出字面单斜杠u16进制,先toString再replace
        String emojiJson = emojis.toString(4).replaceAll("\\\\\\\\u", "\\\\u");
//        String emojiJson = emojis.toString(4);

        File emojiFile = new File(emojiJsonPath);
        System.out.println("save to: " + emojiFile.getAbsolutePath());
        Files.write(emojiFile.toPath(), Collections.singleton(emojiJson), StandardCharsets.UTF_8);
    }

    private static String getProjectPath() {
        String projectPath = Objects.requireNonNull(JsonGenerator.class.getResource("")).getPath();
        projectPath = projectPath.substring(0, projectPath.indexOf("/target"));
        return projectPath;
    }
    /**
     * convert emoji to unicode
     *
     * @param emoji emoji char
     * @return emoji's unicode
     */
    private static String convertEmoji2Unicode(String emoji) {
        char[] chars = emoji.toCharArray();
        StringBuilder builder = new StringBuilder();
        for (char c : chars) {
            // 不足补0
            builder.append("\\u").append(Integer.toHexString(0x10000 | c).substring(1).toUpperCase());
        }
        return builder.toString();
    }

    /**
     * jsoup document builder
     *
     * @return access url and get body when args without `path` arg else read local file by `path`
     * @throws IOException io exception
     */
    private static Document getDocument() throws IOException {
        if (isBlank(OFFLINE_PATH)) {
            return getConnection().get();
        } else {
            return Jsoup.parse(new File(OFFLINE_PATH), "utf-8", "https://unicode.org/");
        }
    }

    /**
     * builder jsoup connection
     *
     * @return jsoup connection
     */
    private static Connection getConnection() {
        Connection connect = Jsoup.connect(ONLINE_URL).maxBodySize(Integer.MAX_VALUE);
        if (!isBlank(PROXY_HOST) && !isBlank(PROXY_PORT)) {
            connect.proxy(PROXY_HOST, Integer.parseInt(PROXY_PORT));
        }
        return connect;
    }

    /**
     * like apache commons lang 3 StringUtils.isBlank
     *
     * @param str check string
     * @return blank is true,else false
     */
    private static boolean isBlank(String str) {
        return Objects.isNull(str) || str.trim().isEmpty();
    }

    private static Map<String, JSONObject> getJsonMapFromEmojiJson(String emojiPath) throws IOException {
        if (Objects.isNull(emojiPath) || emojiPath.isEmpty()) {
            return Collections.emptyMap();
        }
        JSONArray emojiArray = new JSONArray(String.join("", Files.readAllLines(new File(emojiPath).toPath())));
        Map<String, JSONObject> emojiMap = new HashMap<>(emojiArray.length());
        for (Object json : emojiArray) {
            JSONObject emoji = (JSONObject) json;
            emojiMap.put(emoji.getString("emojiChar"), emoji);
        }
        return emojiMap;
    }

    private static Map<String, String> getI18nMapFromEmojiI18nJson(String emojiI18nPath) throws IOException {
        if (Objects.isNull(emojiI18nPath) || emojiI18nPath.isEmpty()) {
            return Collections.emptyMap();
        }
		JSONArray emojiArray = new JSONArray(String.join("", Files.readAllLines(new File(emojiI18nPath).toPath())));
		Map<String, String> emojiMap = new HashMap<>(emojiArray.length());
        for (Object json : emojiArray) {
            JSONObject emoji = (JSONObject) json;
            emojiMap.put(emoji.getString("emojiChar"), emoji.getString("description"));
        }
        return emojiMap;
    }
}
