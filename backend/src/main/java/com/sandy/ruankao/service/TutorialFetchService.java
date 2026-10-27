package com.sandy.ruankao.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

@Service
/** @author zoujunke */
public class TutorialFetchService {
  private static final Logger log = LoggerFactory.getLogger(TutorialFetchService.class);
  private static final String USER_AGENT = "RuankaoStudyBot/1.0 (+tutorial-source-sync)";
  private static final int MAX_BYTES = 2 * 1024 * 1024;

  public FetchResult fetch(String rawUrl) throws Exception {
    URL url = validatePublicUrl(rawUrl);
    if (!robotsAllowed(url)) throw new IllegalArgumentException("来源网站 robots.txt 不允许抓取该页面");
    long start = System.currentTimeMillis();
    Connection.Response response = null;
    for (int redirect = 0; redirect <= 3; redirect++) {
      response =
          Jsoup.connect(url.toString())
              .userAgent(USER_AGENT)
              .timeout(10000)
              .maxBodySize(MAX_BYTES)
              .ignoreHttpErrors(true)
              .followRedirects(false)
              .execute();
      int status = response.statusCode();
      if (status >= 300 && status < 400) {
        String location = response.header("Location");
        if (location == null) throw new IllegalArgumentException("网页重定向缺少目标地址");
        url = validatePublicUrl(new URL(url, location).toString());
        continue;
      }
      if (status < 200 || status >= 300) throw new IllegalArgumentException("网页返回 HTTP " + status);
      String type = Objects.toString(response.contentType(), "").toLowerCase();
      if (!type.contains("text/html")) throw new IllegalArgumentException("来源不是 HTML 网页");
      FetchResult result = parseHtml(response.parse().html(), url.toString());
      result.httpStatus = status;
      result.durationMs = System.currentTimeMillis() - start;
      log.info("教程网页抓取完成 status={} durationMs={} sectionCount={}",
          status, result.durationMs, result.sections.size());
      return result;
    }
    throw new IllegalArgumentException("网页重定向次数过多");
  }

  public FetchResult parseHtml(String html, String baseUri) throws Exception {
    Document doc = Jsoup.parse(html, baseUri);
    doc.select(
            "script,style,noscript,iframe,form,button,nav,footer,aside,.advertisement,.ads,.comment")
        .remove();
    Element root = doc.selectFirst("article");
    if (root == null) root = doc.selectFirst("main");
    if (root == null) root = doc.body();
    if (root == null || root.text().trim().length() < 80)
      throw new IllegalArgumentException("未能提取到足够的教程正文");
    String title = doc.title().trim();
    Element h1 = root.selectFirst("h1");
    if (h1 != null && !h1.text().trim().isEmpty()) title = h1.text().trim();
    if (title.isEmpty()) title = "未命名教程";
    List<SectionData> sections = new ArrayList<>();
    String currentTitle = "导读";
    List<Node> nodes = new ArrayList<>();
    Map<String, Integer> duplicate = new HashMap<>();
    for (Node node : new ArrayList<>(root.childNodes())) {
      if (node instanceof Element
          && (((Element) node).tagName().equals("h2") || ((Element) node).tagName().equals("h3"))) {
        addSection(sections, currentTitle, nodes, baseUri, duplicate);
        currentTitle = ((Element) node).text().trim();
        nodes = new ArrayList<>();
      } else nodes.add(node.clone());
    }
    addSection(sections, currentTitle, nodes, baseUri, duplicate);
    if (sections.isEmpty()) throw new IllegalArgumentException("网页正文没有可发布的小节");
    String summary = doc.select("meta[name=description]").attr("content").trim();
    if (summary.isEmpty()) summary = root.text().substring(0, Math.min(180, root.text().length()));
    StringBuilder hashInput = new StringBuilder(title);
    for (SectionData s : sections) hashInput.append(s.title).append(s.html);
    return new FetchResult(title, summary, sha256(hashInput.toString()), sections);
  }

  public URL validatePublicUrl(String raw) throws Exception {
    URL url = new URL(raw);
    if (!"https".equalsIgnoreCase(url.getProtocol()))
      throw new IllegalArgumentException("教程来源必须使用 HTTPS");
    if (url.getUserInfo() != null || url.getHost() == null || url.getHost().trim().isEmpty())
      throw new IllegalArgumentException("来源网址无效");
    for (InetAddress address : InetAddress.getAllByName(url.getHost())) {
      byte[] b = address.getAddress();
      boolean ula = b.length == 16 && ((b[0] & 0xfe) == 0xfc);
      boolean linkLocalV6 = b.length == 16 && (b[0] & 0xff) == 0xfe && ((b[1] & 0xc0) == 0x80);
      if (address.isAnyLocalAddress()
          || address.isLoopbackAddress()
          || address.isSiteLocalAddress()
          || address.isLinkLocalAddress()
          || address.isMulticastAddress()
          || ula
          || linkLocalV6) throw new IllegalArgumentException("禁止访问本机或内网地址");
    }
    return url;
  }

  private boolean robotsAllowed(URL target) {
    try {
      URL robots =
          validatePublicUrl(
              new URL(target.getProtocol(), target.getHost(), target.getPort(), "/robots.txt")
                  .toString());
      Connection.Response r =
          Jsoup.connect(robots.toString())
              .userAgent(USER_AGENT)
              .timeout(4000)
              .maxBodySize(256 * 1024)
              .ignoreHttpErrors(true)
              .followRedirects(false)
              .execute();
      if (r.statusCode() != 200) return true;
      boolean applies = false;
      for (String raw : r.body().split("\\r?\\n")) {
        String line = raw.split("#", 2)[0].trim();
        if (line.toLowerCase().startsWith("user-agent:"))
          applies = line.substring(11).trim().equals("*");
        else if (applies && line.toLowerCase().startsWith("disallow:")) {
          String path = line.substring(9).trim();
          if (!path.isEmpty() && target.getPath().startsWith(path)) return false;
        }
      }
      return true;
    } catch (Exception ignored) {
      log.debug("robots 校验未完成，沿用允许抓取策略");
      return true;
    }
  }

  private void addSection(
      List<SectionData> out,
      String title,
      List<Node> nodes,
      String baseUri,
      Map<String, Integer> duplicate)
      throws Exception {
    Element holder = new Element("div");
    for (Node n : nodes) holder.appendChild(n);
    String text = holder.text().trim();
    if (text.length() < 20) return;
    holder.select("a[href]").attr("target", "_blank").attr("rel", "noopener noreferrer");
    for (Element img : holder.select("img[src]")) {
      String abs = img.absUrl("src");
      if (!abs.startsWith("https://")) img.remove();
      else img.attr("src", abs);
    }
    Safelist safe =
        Safelist.relaxed()
            .addTags("table", "thead", "tbody", "tr", "th", "td", "pre", "code")
            .addAttributes("a", "target", "rel", "title")
            .addAttributes("img", "alt", "title")
            .addProtocols("a", "href", "https")
            .addProtocols("img", "src", "https");
    String clean =
        Jsoup.clean(holder.html(), baseUri, safe, new Document.OutputSettings().prettyPrint(false));
    if (Jsoup.parse(clean).text().trim().length() < 20) return;
    String base = sha256(title.trim().toLowerCase()).substring(0, 24);
    int n = duplicate.getOrDefault(base, 0) + 1;
    duplicate.put(base, n);
    out.add(new SectionData(base + (n > 1 ? "-" + n : ""), title, clean, out.size() + 1));
  }

  private static String sha256(String value) throws Exception {
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    byte[] bytes = md.digest(value.getBytes(StandardCharsets.UTF_8));
    StringBuilder s = new StringBuilder();
    for (byte b : bytes) s.append(String.format("%02x", b));
    return s.toString();
  }

  public static class FetchResult {
    public final String title, summary, hash;
    public final List<SectionData> sections;
    public int httpStatus;
    public long durationMs;

    FetchResult(String title, String summary, String hash, List<SectionData> sections) {
      this.title = title;
      this.summary = summary;
      this.hash = hash;
      this.sections = sections;
    }
  }

  public static class SectionData {
    public final String key, title, html;
    public final int sequence;

    SectionData(String key, String title, String html, int sequence) {
      this.key = key;
      this.title = title;
      this.html = html;
      this.sequence = sequence;
    }
  }
}
