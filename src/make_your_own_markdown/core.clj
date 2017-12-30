
(ns make-your-own-markdown.core
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [instaparse.core :as insta]
            [hiccup.core :as hiccup]
            [hiccup.page :refer [html5 include-css]]
            [me.raynes.fs :as fs])
  (:gen-class))


(def markup-to-tree
  (insta/parser
   "<root> = (heading |
              anchor |
              paragraph |
              indented |
              table
             )+

    heading = #'[#]+' <space> formatted-text <newlines>
    paragraph = formatted-text  <blankline> <newline>*
    indented = #'[ ]+' formatted-text <newline>+
    <formatted-text> = (emphasis | strong | br | text)+
    <text> = #'[^#*_\\|~\n]+'

    table = tablerow tableformat tablerow+ <newline>+
    tablerow = (formatted-text column <spaces>)+ formatted-text <space>? <column>? <newline>
    tableformat = <dashes> (column-marker <dashes>)+ <newline>
    <dashes> = dash+
    <dash> = '-'
    <column-marker> = <spaces> column <spaces>
    <column> = '|'

    strong = <'*'> strong-text <'*'>
    <strong-text> = #'[^\\*]+'
    emphasis =  <'_'> emphasis-text <'_'>
    <emphasis-text> = #'[^_]+'

    anchor = auto-anchor | braced-anchor
    <auto-anchor> = <'<'> url <'>'>
    <braced-anchor> = <'['> anchor-text <']'> <'('> url <')'>
    <anchor-text> = #'[^]]+'
    <url> = #'[^>)]+'

    blankline = #'\n\n'
    <newlines> = newline+
    newline = #'\n'
    spaces = space+
    space = ' '
    br = #'~'
    "))


;; Transformers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; [:anchor "example.com"]
;; [:anchor "Click me" "example.com" ]
(defn transform-anchor
  ([url] [:a {:href url} url])
  ([text url] [:a {:href url} text]))

;; [:emphasis "lol"]
(defn transform-emphasis
  [text]
  [:em text])


(defn transform-br
  [_]
  [:br])

;; [:strong "lol"]
(defn transform-strong
  [text]
  [:strong text])


(defn transform-paragraph
  [& items]
  (into [:p] items))


(defn transform-indented
  [spaces & text]
  (let [level (count spaces)]
    (into [:div {:class (str "level-" level)}]  text)))

(defn transform-heading
  [octothorpes & text]
  (let [level (count octothorpes)
        tag (keyword (str "h" level))]
    (into [tag ] text)))


(defn split-row [row]
  (let [cells (lazy-seq (split-with (complement  #{"|"}) row))]
    (if (empty? (second cells))
      cells
      (cons (first cells) (split-row (rest (second cells)))))))

(defn row [row]
  (let [cells (split-row (drop 1 row))]
    [:tr (map #(into [:td] %) cells)]))

(defn transform-table [header formatting & rows]
  [:table
    [:thead
     (row header)]
    [:tbody
      (map row rows)]])


;; Usage ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn tree-to-hiccup
  [tree]
  (let [transformations {:anchor transform-anchor
                         :br transform-br
                         :emphasis transform-emphasis
                         :strong transform-strong
                         :heading transform-heading
                         :indented transform-indented
                         :table transform-table
                         :paragraph transform-paragraph}]
    (insta/transform transformations tree)))



(defn make-page [title body]
  (html5
   [:head
    [:meta {:charset "utf-8"}]
    [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge,chrome=1"}]
    [:meta {:name "viewport" :content
            "width=device-width, initial-scale=1, maximum-scale=1"}]
    [:title (string/capitalize  (string/replace title #"-" " "))]
    (include-css "/stylesheets/base.css")
    (include-css "http://fonts.googleapis.com/css?family=Sigmar+One&v1")]
   [:body
    [:div {:id "container"}
      [:div {:id "content" :class "container"} body]]]))


(defn parse [markup]
  (let [parsed (markup-to-tree markup)]
    (when (insta/failure? parsed)
        (print parsed))
    parsed))

(defn markdown-to-html [filename markup]
  (make-page filename
    (hiccup/html (tree-to-hiccup
                    (parse markup)))))

(defn -main [path & args]
  (let [filename (fs/base-name path true)]
    (spit (str filename ".html") (markdown-to-html filename (slurp path)))))


