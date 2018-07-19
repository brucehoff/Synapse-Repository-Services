package org.sagebionetworks.doi.datacite;

import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;
import org.apache.xerces.dom.DocumentImpl;
import org.sagebionetworks.repo.model.doi.v2.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import static org.sagebionetworks.doi.datacite.DataciteMetadataConstants.*;
import static org.sagebionetworks.doi.datacite.DataciteUtils.generateDoiUri;
import static org.sagebionetworks.doi.datacite.DataciteUtils.getSchemeUri;

/*
 * Translates our DoiV2 object into well-formed DataCite XML.
 */
public class DataciteMetadataTranslatorImpl implements DataciteMetadataTranslator {

	public String translate(Doi doi) {
		return translateUtil(doi);
	}

	static String translateUtil(Doi doi) {
		Document dom = createXmlDom(doi);
		return xmlToString(dom);
	}

	static String xmlToString(Document dom) {
		OutputFormat format = new OutputFormat(dom);
		StringWriter xml = new StringWriter();
		XMLSerializer serializer = new XMLSerializer(xml, format);

		try {
			serializer.serialize(dom);
		} catch (IOException e) {
			throw new RuntimeException("Error occurred while serializing metadata", e);
		}
		return xml.toString();
	}

	static Document createXmlDom(Doi doi) {
		Document dom = new DocumentImpl();
		Element resource = dom.createElement(RESOURCE);
		resource.setAttribute(NAMESPACE_PREFIX, NAMESPACE_PREFIX_VALUE);
		resource.setAttribute(NAMESPACE, NAMESPACE_VALUE);
		resource.setAttribute(SCHEMA_LOCATION, SCHEMA_LOCATION_VALUE);
		dom.appendChild(resource);

		resource.appendChild(createIdentifierElement(dom, generateDoiUri(doi)));
		resource.appendChild(createCreatorsElement(dom, doi.getCreators()));
		resource.appendChild(createTitlesElement(dom, doi.getTitles()));
		resource.appendChild(createPublisherElement(dom));
		resource.appendChild(createPublicationYearElement(dom, String.valueOf(doi.getPublicationYear())));
		resource.appendChild(createResourceTypeElement(dom, doi.getResourceType()));
		return dom;
	}


	static Element createIdentifierElement(Document dom, String doi) {
		Element identifier = dom.createElement(IDENTIFIER);
		identifier.setAttribute(IDENTIFIER_TYPE, IDENTIFIER_TYPE_VALUE);
		identifier.setTextContent(doi);
		return identifier;
	}

	static Element createCreatorElement(Document dom, DoiCreator creator) {
		Element creatorElement = dom.createElement(CREATOR);
		Element creatorName = dom.createElement(CREATOR_NAME);
		creatorName.setTextContent(creator.getCreatorName());
		creatorElement.appendChild(creatorName);
		if (creator.getNameIdentifiers() != null) {
			for (DoiNameIdentifier nameIdentifier : creator.getNameIdentifiers()) {
				Element nameIdElement = dom.createElement(NAME_IDENTIFIER);
				nameIdElement.setTextContent(nameIdentifier.getIdentifier());
				nameIdElement.setAttribute(NAME_IDENTIFIER_SCHEME, nameIdentifier.getNameIdentifierScheme().name());
				nameIdElement.setAttribute(SCHEME_URI, getSchemeUri(nameIdentifier.getNameIdentifierScheme()));
				creatorElement.appendChild(nameIdElement);
			}
		}
		return creatorElement;
	}

	static Element createCreatorsElement(Document dom, List<DoiCreator> creators) {
		Element creatorsElement = dom.createElement(CREATORS);
		for (DoiCreator creator : creators) {
			creatorsElement.appendChild(createCreatorElement(dom, creator));
		}
		return creatorsElement;
	}

	static Element createTitleElement(Document dom, DoiTitle title) {
		Element titleElement = dom.createElement(TITLE);
		titleElement.setTextContent(title.getTitle());
		return titleElement;
	}

	static Element createTitlesElement(Document dom, List<DoiTitle> titles) {
		Element titlesElement = dom.createElement(TITLES);
		for (DoiTitle title : titles) {
			titlesElement.appendChild(createTitleElement(dom, title));
		}
		return titlesElement;
	}

	static Element createPublisherElement(Document dom) {
		Element publisher = dom.createElement(PUBLISHER);
		publisher.setTextContent(PUBLISHER_VALUE);
		return publisher;
	}

	static Element createPublicationYearElement(Document dom, String publicationYear) {
		Element publicationYearElement = dom.createElement(PUBLICATION_YEAR);
		publicationYearElement.setTextContent(publicationYear);
		return publicationYearElement;
	}

	static Element createResourceTypeElement(Document dom, DoiResourceType resourceType) {
		Element resourceTypeElement = dom.createElement(RESOURCE_TYPE);
		resourceTypeElement.setAttribute(RESOURCE_TYPE_GENERAL, resourceType.getResourceTypeGeneral().name());
		return resourceTypeElement;
	}
}