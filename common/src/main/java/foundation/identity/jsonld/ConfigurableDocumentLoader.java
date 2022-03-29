package foundation.identity.jsonld;

import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.document.Document;
import com.apicatalog.jsonld.document.JsonDocument;
import com.apicatalog.jsonld.http.DefaultHttpClient;
import com.apicatalog.jsonld.loader.DocumentLoader;
import com.apicatalog.jsonld.loader.DocumentLoaderOptions;
import com.apicatalog.jsonld.loader.FileLoader;
import com.apicatalog.jsonld.loader.HttpLoader;
import com.github.benmanes.caffeine.cache.Cache;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class ConfigurableDocumentLoader implements DocumentLoader {

    private static DocumentLoader DEFAULT_HTTP_LOADER;
    private static DocumentLoader DEFAULT_FILE_LOADER;

    private DocumentLoader httpLoader;
    private DocumentLoader fileLoader;

    private boolean enableLocalCache = true;
    private boolean enableHttp = false;
    private boolean enableHttps = false;
    private boolean enableFile = false;

    private Map<URI, JsonDocument> localCache = new HashMap<URI, JsonDocument> ();
    private Cache<URI, Document> remoteCache = null;
    private List<URI> httpContexts = new ArrayList<URI>();
    private List<URI> httpsContexts = new ArrayList<URI>();
    private List<URI> fileContexts = new ArrayList<URI>();

    public static final DocumentLoader DOCUMENT_LOADER;

    static {

        DOCUMENT_LOADER = new ConfigurableDocumentLoader();
    }

    public static DocumentLoader getDefaultHttpLoader() {
        if (DEFAULT_HTTP_LOADER == null) DEFAULT_HTTP_LOADER = new HttpLoader(DefaultHttpClient.defaultInstance());
        return DEFAULT_HTTP_LOADER;
    }

    public static DocumentLoader getDefaultFileLoader() {
        if (DEFAULT_FILE_LOADER == null) DEFAULT_FILE_LOADER = new FileLoader();
        return DEFAULT_FILE_LOADER;
    }

    public static void setDefaultHttpLoader(DocumentLoader defaultHttpLoader) {
        DEFAULT_HTTP_LOADER = defaultHttpLoader;
    }

    public static void setDefaultFileLoader(DocumentLoader defaultFileLoader) {
        DEFAULT_FILE_LOADER = defaultFileLoader;
    }

    public ConfigurableDocumentLoader() {

    }

    public ConfigurableDocumentLoader(Map<URI, JsonDocument> localCache) {
        if (localCache == null) throw new NullPointerException();
        this.localCache = localCache;
    }

    @Override
    public Document loadDocument(URI url, DocumentLoaderOptions options) throws JsonLdError {

        if (this.isEnableLocalCache() && this.getLocalCache().containsKey(url)) {
            return this.getLocalCache().get(url);
        }
        if (this.isEnableHttp() && "http".equalsIgnoreCase(url.getScheme())) {
            if (!this.getHttpContexts().isEmpty() && !this.getHttpContexts().contains(url)) return null;
            DocumentLoader httpLoader = this.getHttpLoader();
            if (httpLoader == null) httpLoader = getDefaultHttpLoader();
            Document document = this.getRemoteCache() == null ? null : this.getRemoteCache().getIfPresent(url);
            if (document == null) {
                document = httpLoader.loadDocument(url, options);
                if (this.getRemoteCache() != null) this.getRemoteCache().put(url, document);
            }
            return document;
        }
        if (this.isEnableHttps() && "https".equalsIgnoreCase(url.getScheme())) {
            if (!this.getHttpsContexts().isEmpty() && !this.getHttpsContexts().contains(url)) return null;
            DocumentLoader httpLoader = this.getHttpLoader();
            if (httpLoader == null) httpLoader = getDefaultHttpLoader();
            Document document = this.getRemoteCache() == null ? null : this.getRemoteCache().getIfPresent(url);
            if (document == null) {
                document = httpLoader.loadDocument(url, options);
                if (this.getRemoteCache() != null) this.getRemoteCache().put(url, document);
            }
            return document;
        }
        if (this.isEnableFile() && "file".equalsIgnoreCase(url.getScheme())) {
            if (!this.getFileContexts().isEmpty() && !this.getFileContexts().contains(url)) return null;
            DocumentLoader fileLoader = this.getFileLoader();
            if (fileLoader == null) fileLoader = getDefaultFileLoader();
            Document document = this.getRemoteCache() == null ? null : this.getRemoteCache().getIfPresent(url);
            if (document == null) {
                document = fileLoader.loadDocument(url, options);
                if (this.getRemoteCache() != null) this.getRemoteCache().put(url, document);
            }
            return document;
        }

        Logger.getLogger(this.getClass().getName()).warning("Cannot load context: " + url);
        return null;
    }

    /*
     * Getters and setters
     */

    public DocumentLoader getHttpLoader() {
        return this.httpLoader;
    }

    public void setHttpLoader(DocumentLoader httpLoader) {
        this.httpLoader = httpLoader;
    }

    public DocumentLoader getFileLoader() {
        return this.fileLoader;
    }

    public void setFileLoader(DocumentLoader fileLoader) {
        this.fileLoader = fileLoader;
    }

    public boolean isEnableLocalCache() {
        return this.enableLocalCache;
    }

    public void setEnableLocalCache(boolean enableLocalCache) {
        this.enableLocalCache = enableLocalCache;
    }

    public boolean isEnableHttp() {
        return this.enableHttp;
    }

    public void setEnableHttp(boolean enableHttp) {
        this.enableHttp = enableHttp;
    }

    public boolean isEnableHttps() {
        return this.enableHttps;
    }

    public void setEnableHttps(boolean enableHttps) {
        this.enableHttps = enableHttps;
    }

    public boolean isEnableFile() {
        return this.enableFile;
    }

    public void setEnableFile(boolean enableFile) {
        this.enableFile = enableFile;
    }

    public Map<URI, JsonDocument> getLocalCache() {
        return this.localCache;
    }

    public void setLocalCache(Map<URI, JsonDocument> localCache) {
        this.localCache = localCache;
    }

    public Cache<URI, Document> getRemoteCache() {
        return this.remoteCache;
    }

    public void setRemoteCache(Cache<URI, Document> remoteCache) {
        this.remoteCache = remoteCache;
    }

    public List<URI> getHttpContexts() {
        return this.httpContexts;
    }

    public void setHttpContexts(List<URI> httpContexts) {
        this.httpContexts = httpContexts;
    }

    public List<URI> getHttpsContexts() {
        return this.httpsContexts;
    }

    public void setHttpsContexts(List<URI> httpsContexts) {
        this.httpsContexts = httpsContexts;
    }

    public List<URI> getFileContexts() {
        return this.fileContexts;
    }

    public void setFileContexts(List<URI> fileContexts) {
        this.fileContexts = fileContexts;
    }
}
