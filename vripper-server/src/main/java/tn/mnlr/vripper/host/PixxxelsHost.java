package tn.mnlr.vripper.host;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.protocol.HttpClientContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import tn.mnlr.vripper.exception.HostException;
import tn.mnlr.vripper.exception.XpathException;
import tn.mnlr.vripper.services.HostService;
import tn.mnlr.vripper.services.XpathService;

@Service
@Slf4j
public class PixxxelsHost extends Host {

    private static final String host = "pixxxels.cc";
    private static final String IMG_XPATH = "//*[@id='download']";
    private static final String TITLE_XPATH = "//*[contains(@class,'imagename')]";

    private final HostService hostService;
    private final XpathService xpathService;

    @Autowired
    public PixxxelsHost(HostService hostService, XpathService xpathService) {
        this.hostService = hostService;
        this.xpathService = xpathService;
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getLookup() {
        return host;
    }

    @Override
    public HostService.NameUrl getNameAndUrl(final String _url, final HttpClientContext context) throws HostException {

        String url = _url.replace("http://", "https://");
        HostService.Response resp = hostService.getResponse(url, context);
        Document doc = resp.getDocument();

        Node imgNode, titleNode;
        try {
            log.debug(String.format("Looking for xpath expression %s in %s", IMG_XPATH, url));
            imgNode = xpathService.getAsNode(doc, IMG_XPATH);

            log.debug(String.format("Looking for xpath expression %s in %s", TITLE_XPATH, url));
            titleNode = xpathService.getAsNode(doc, TITLE_XPATH);

        } catch (XpathException e) {
            throw new HostException(e);
        }

        try {
            log.debug(String.format("Resolving name and image url for %s", url));
            String imgTitle = titleNode.getTextContent().trim();
            String imgUrl = imgNode.getAttributes().getNamedItem("href").getTextContent().trim();

            return new HostService.NameUrl(imgTitle.isEmpty() ? imgUrl.substring(imgUrl.lastIndexOf('/') + 1) : imgTitle, imgUrl);
        } catch (Exception e) {
            throw new HostException("Unexpected error occurred", e);
        }
    }
}
