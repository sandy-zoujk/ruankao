export function sanitizeHtml(html) {
  const doc = new DOMParser().parseFromString(html || "", "text/html");
  const allowed = new Set([
    "P",
    "BR",
    "DIV",
    "SPAN",
    "STRONG",
    "B",
    "EM",
    "I",
    "U",
    "UL",
    "OL",
    "LI",
    "BLOCKQUOTE",
    "PRE",
    "CODE",
    "TABLE",
    "THEAD",
    "TBODY",
    "TR",
    "TH",
    "TD",
    "A",
    "IMG",
  ]);
  [...doc.body.querySelectorAll("*")].forEach((el) => {
    if (!allowed.has(el.tagName)) {
      el.replaceWith(...el.childNodes);
      return;
    }
    [...el.attributes].forEach((a) => {
      const ok =
        (el.tagName === "A" &&
          ["href", "target", "rel", "title"].includes(a.name)) ||
        (el.tagName === "IMG" && ["src", "alt", "title"].includes(a.name));
      if (!ok) el.removeAttribute(a.name);
    });
    if (el.tagName === "A") {
      if (!el.href.startsWith("https://")) el.removeAttribute("href");
      el.target = "_blank";
      el.rel = "noopener noreferrer";
    }
    if (el.tagName === "IMG" && !el.src.startsWith("https://")) el.remove();
  });
  return doc.body.innerHTML;
}
