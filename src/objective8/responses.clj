(ns objective8.responses
  (:refer-clojure :exclude [comment])
  (:require [net.cgrand.enlive-html :as html]
            [objective8.translation :refer [translation-config]]
            [objective8.config :as config]
            [objective8.utils :as utils]
            [ring.util.anti-forgery :refer [anti-forgery-field]]))

(def google-analytics-tracking-id (config/get-var "GA_TRACKING_ID"))

(defn objective-url [objective]
  (str utils/host-url "/objectives/" (:_id objective)))

(defn text->p-nodes
  "Turns text into a collection of paragraph nodes based on linebreaks.
   Returns nil if no text is supplied"
  [text]
  (when text
    (let [newline-followed-by-optional-whitespace #"(\n+|\r+)\s*"]
    (map (fn [p] (html/html [:p p])) (clojure.string/split text
                                                           newline-followed-by-optional-whitespace)))))

;GOOGLE ANALYTICS
(html/defsnippet google-analytics
  "templates/google-analytics.html" [[:#clj-google-analytics]] [tracking-id]
  (html/transform-content (html/replace-vars {:trackingID tracking-id})))

;FLASH MESSAGES
(html/defsnippet flash-message-view
  "templates/flash-message.html" [[:#clj-flash-message]] [message]
  [:p] (html/html-content message))

;NAVIGATION
(html/defsnippet objectives-navigation
  "templates/objectives-nav.html" [[:#navigation]] [objective translation]
  [:#clj-objective-title] (html/html-content (:title objective))
  [:#clj-objectives-details] (html/set-attr :href (str "/objectives/" (:_id objective)))
  [:#navigation html/any-node] (html/replace-vars translation))

(html/defsnippet user-navigation-signed-in
  "templates/user-navigation/signed-in.html" [[:#clj-user-navigation]] [{:keys [translation]}]
  [:#clj-user-navigation html/any-node] (html/replace-vars translation))

(html/defsnippet user-navigation-signed-out
  "templates/user-navigation/signed-out.html" [[:#clj-user-navigation]] [{:keys [translation]}]
  [:#clj-user-navigation html/any-node] (html/replace-vars translation))

;BASE TEMPLATE
(html/deftemplate base
  "templates/base.html" [{:keys [translation locale doc-title doc-description user-navigation flash-message content objective]}]
  [:html] (html/set-attr :lang locale)
  ; TODO find a way to select description without an ID
  ; [:head (html/attr= :name "description")] (html/set-attr :content "some text")
  [:title] (html/content doc-title)
  [:#clj-description] (html/set-attr :content doc-description)
  [:.masthead] (html/append user-navigation)
  [:.browserupgrade] (html/html-content (translation :base/browsehappy))
  [:.header-logo] (html/content (translation :base/header-logo-text))
  [:.header-logo] (html/set-attr :title (translation :base/header-logo-title))
  [:#projectStatus] (html/html-content (translation :base/project-status))
  [:#main-content] (html/before (if flash-message (flash-message-view flash-message)))
  [:#main-content] (html/content content)
  [:#clj-navigation] (if objective (html/content (objectives-navigation objective translation)) identity)
  [:body] (html/append (if google-analytics-tracking-id (google-analytics google-analytics-tracking-id))))

;HOME/INDEX
(html/defsnippet index-page
  "templates/index.html" [[:#clj-index]] [{:keys [translation signed-in]}]
  [:.index-get-started] (if signed-in (html/html-content (translation :index/index-get-started-signed-in)) (html/html-content (translation :index/index-get-started-signed-out)))
  [:.index-get-started] (if signed-in (html/set-attr :title (translation :index/index-get-started-title-signed-in)) (html/set-attr :title (translation :index/index-get-started-title-signed-out)))
  [:#clj-index html/any-node] (html/replace-vars translation))

;SIGN IN
(html/defsnippet sign-in-twitter
  "templates/sign-in-twitter.html" [[:#clj-sign-in-twitter]] [])

(html/defsnippet sign-in-page
  "templates/sign-in.html" [[:#clj-sign-in-page]] [{:keys [translation]}]
  [:h1] (html/after (sign-in-twitter))
  [:#clj-sign-in-page html/any-node] (html/replace-vars translation))

;PROJECT STATUS
(html/defsnippet project-status-page
  "templates/project-status.html" [[:#clj-project-status]] [{:keys [translation]}]
  [:#clj-project-status html/any-node] (html/replace-vars translation)
  [:#clj-project-status-detail] (html/html-content (translation :project-status/page-content)))

;ERROR 404
(html/defsnippet error-404-page
  "templates/error-404.html" [:#clj-error-404] [{:keys [translation]}]
  [:#clj-error-404 html/any-node] (html/replace-vars translation)
  [:#clj-error-404-content] (html/html-content (translation :error-404/page-content)))

;SHARING
(html/defsnippet share-widget
  "templates/share-widget.html"
  [:.share-widget] [translation url title]
  [:.share-widget html/any-node] (html/replace-vars translation)
  [:.btn-facebook] (html/set-attr :href (str "http://www.facebook.com/sharer.php?u=" url "t=" title " - "))
  [:.btn-google-plus] (html/set-attr :href (str "https://plusone.google.com/_/+1/confirm?hl=en&url=" url))
  [:.btn-twitter] (html/set-attr :href (str "https://twitter.com/share?url=" url "&text=" title " - "))
  [:.btn-linkedin] (html/set-attr :href (str "http://www.linkedin.com/shareArticle?mini=true&url=" url))
  [:.btn-reddit] (html/set-attr :href (str "http://reddit.com/submit?url=" url "&title=" title " - "))
  [:.share-this-url] (html/set-attr :value url))

;QUESTIONS
(html/defsnippet question-add-page
  "templates/question-add.html" [:#clj-question-add] [{:keys [translation objective-title objective-id]}]
  [:form] (html/prepend (html/html-snippet (anti-forgery-field)))
  [:form] (html/set-attr :action (str "/objectives/" objective-id "/questions"))
  [:h1] (html/content (str (translation :question-add/page-title) ": " objective-title))
  [:#clj-question-add html/any-node] (html/replace-vars translation))

(html/defsnippet question-view-page
  "templates/question-view.html" [:#clj-question-view] [{:keys [translation question]}]
  [:#clj-question-view :h1] (html/content (:question question))
  ; [:.grid] (html/content (share-widget translation (:url question) (:question question)))
  [:#clj-question-view html/any-node] (html/replace-vars translation))

;COMMENTS
(html/defsnippet comment-create
  "templates/comments/comment-create.html" [[:#clj-comment-create]] [objective-id]
  [:form] (html/prepend (html/html-snippet (anti-forgery-field)))
  [:#objective-id] (html/set-attr :value objective-id))

(html/defsnippet a-comment
  "templates/comments/comment.html" [:li] [comment]
  [:.comment-text] (html/content (text->p-nodes (:comment comment)))
  [:.comment-author] (html/content "user-display-name")
  [:.comment-date] (html/content (utils/iso-time-string->pretty-time (:_created_at comment))))

(html/defsnippet comments-view
  "templates/comments/comments-view.html" [[:#clj-comments-view]] [translation signed-in objective-id comments uri]
  [:#clj-comment-sign-in-uri] #(assoc-in % [:attrs :href] (str "/sign-in?refer=" uri))
  [:#clj-comments-view :.response-form] (if signed-in (html/content (comment-create objective-id)) identity )
  [:#clj-comments-view html/any-node] (html/replace-vars translation)
  [:#clj-comments-view :.comment-list] (if (empty? comments) identity (html/content (map a-comment comments))))

;OBJECTIVES
(html/defsnippet a-goal
  "templates/goal.html" [:li] [goal]
  [:li] (html/content goal))

(html/defsnippet objective-list-page
  "templates/objectives-list.html" [[:#clj-objectives-list]] [{:keys [translation]}]
  [:#clj-objectives-list html/any-node] (html/replace-vars translation))

(html/defsnippet objective-create-page
  "templates/objectives-create.html" [[:#clj-objective-create]] [{:keys [translation]}]
  [:form] (html/prepend (html/html-snippet (anti-forgery-field)))
  [:#clj-objective-create html/any-node] (html/replace-vars translation))

(html/defsnippet objective-detail-page
  "templates/objectives-detail.html" [[:#clj-objectives-detail]]
  [{:keys [translation objective signed-in comments uri]}]
  [:#clj-objectives-detail html/any-node] (html/replace-vars translation)
  [:h1] (html/content (:title objective))
  [:#clj-obj-goals-value] (html/content (map a-goal (:goals objective)))
  [:#clj-obj-background-label] (if (empty? (:description objective)) nil identity)
  [:#clj-obj-background-label] (html/after (text->p-nodes (:description objective)))
  [:#clj-obj-end-date-value] (html/content (:end-date objective))
  [:.share-widget html/any-node] (html/replace-vars translation)
  [:.btn-facebook] (html/set-attr :href (str "http://www.facebook.com/sharer.php?u=" (objective-url objective) "t=" (:title objective) " - "))
  [:.btn-google-plus] (html/set-attr :href (str "https://plusone.google.com/_/+1/confirm?hl=en&url=" (objective-url objective)))
  [:.btn-twitter] (html/set-attr :href (str "https://twitter.com/share?url=" (objective-url objective) "&text=" (:title objective) " - "))
  [:.btn-linkedin] (html/set-attr :href (str "http://www.linkedin.com/shareArticle?mini=true&url=" (objective-url objective)))
  [:.btn-reddit] (html/set-attr :href (str "http://reddit.com/submit?url=" (objective-url objective) "&title=" (:title objective) " - "))
  [:.share-this-url] (html/set-attr :value (objective-url objective))
  [:#clj-objectives-detail] (html/after (comments-view translation signed-in (:_id objective) comments uri)))

;USERS
(html/defsnippet users-email
  "templates/users-email.html" [[:#clj-users-email]] [{:keys [translation]}]
  [:form] (html/prepend (html/html-snippet (anti-forgery-field)))
  [:#clj-users-email html/any-node] (html/replace-vars translation))


(defn render-template [template & args]
  (apply str (apply template args)))

(defn simple-response
  "Returns a response with given status code or 200"
  ([text]
   (simple-response text 200))
  ([text status-code]
   {:status status-code
    :header {"Content-Type" "text/html"}
    :body text}))

(defn rendered-response [template-name args]
  (let [user-navigation (if (:signed-in args)
                             user-navigation-signed-in
                             user-navigation-signed-out)
        page (render-template base (assoc args
                                          :content (template-name args)
                                          :flash-message (:message args)
                                          :user-navigation (user-navigation args)))]
        (if-let [status-code (:status-code args)]
          (simple-response page status-code)
          (simple-response page))))
