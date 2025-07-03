/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.server.healthconnect.phr.validations;

import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_BASE64_BINARY;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_CANONICAL;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_CODE;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_DATE;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_DATE_TIME;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_ID;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_INSTANT;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_MARKDOWN;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_OID;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_POSITIVE_INT;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_STRING;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_TIME;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_UNSIGNED_INT;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_URI;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_URL;
import static com.android.server.healthconnect.proto.R4FhirType.R4_FHIR_TYPE_UUID;

import android.annotation.Nullable;
import android.util.Xml;

import com.android.healthfitness.flags.Flags;
import com.android.server.healthconnect.proto.R4FhirType;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Performs validation on FHIR primitive values.
 *
 * @hide
 */
public class FhirPrimitiveTypeValidator {
    private static final Map<R4FhirType, Integer> sR4PrimitiveIntegerTypeToMinValueMap =
            new HashMap<>();
    private static final Map<R4FhirType, Pattern> sR4PrimitiveStringTypeToPatternMap =
            new HashMap<>();
    private static final Map<String, Set<String>> sXhtmlElementToAttributesAllowlistMap =
            new HashMap<>();

    // All regex below are copied from https://hl7.org/fhir/R4/datatypes.html. Please keep the regex
    // patterns below SORTED.
    private static final Pattern BASE64_BINARY_R4_PATTERN =
            Pattern.compile("(\\s*([0-9a-zA-Z\\+\\=]){4}\\s*)+");
    private static final Pattern CANONICAL_R4_PATTERN = Pattern.compile("\\S*");
    private static final Pattern CODE_R4_PATTERN = Pattern.compile("[^\\s]+(\\s[^\\s]+)*");
    private static final Pattern DATE_R4_PATTERN =
            Pattern.compile(
                    "([0-9]([0-9]([0-9][1-9]|[1-9]0)|[1-9]00)|[1-9]000)"
                            + "(-(0[1-9]|1[0-2])(-(0[1-9]|[1-2][0-9]|3[0-1]))?)?");
    // With the R4 regex, if a time is specified, a timezone offset must be populated.
    private static final Pattern DATE_TIME_R4_PATTERN =
            Pattern.compile(
                    "([0-9]([0-9]([0-9][1-9]|[1-9]0)|[1-9]00)|[1-9]000)"
                            + "(-(0[1-9]|1[0-2])(-(0[1-9]|[1-2][0-9]|3[0-1])"
                            + "(T([01][0-9]|2[0-3]):[0-5][0-9]:([0-5][0-9]|60)"
                            + "(\\.[0-9]+)?(Z|(\\+|-)((0[0-9]|1[0-3]):[0-5][0-9]|14:00)))?)?)?");
    private static final Pattern ID_R4_PATTERN = Pattern.compile("[A-Za-z0-9\\-\\.]{1,64}");
    private static final Pattern INSTANT_R4_PATTERN =
            Pattern.compile(
                    "([0-9]([0-9]([0-9][1-9]|[1-9]0)|[1-9]00)|[1-9]000)"
                            + "-(0[1-9]|1[0-2])-(0[1-9]|[1-2][0-9]|3[0-1])"
                            + "T([01][0-9]|2[0-3]):[0-5][0-9]:([0-5][0-9]|60)"
                            + "(\\.[0-9]+)?(Z|(\\+|-)((0[0-9]|1[0-3]):[0-5][0-9]|14:00))");
    private static final Pattern MARKDOWN_R4_PATTERN = Pattern.compile("\\s*(\\S|\\s)*");
    private static final Pattern OID_R4_PATTERN =
            Pattern.compile("urn:oid:[0-2](\\.(0|[1-9][0-9]*))+");
    private static final Pattern STRING_R4_PATTERN = Pattern.compile("[ \\r\\n\\t\\S]+");
    private static final Pattern TIME_R4_PATTERN =
            Pattern.compile("([01][0-9]|2[0-3]):[0-5][0-9]:([0-5][0-9]|60)(\\.[0-9]+)?");
    private static final Pattern URI_R4_PATTERN = Pattern.compile("\\S*");
    private static final Pattern URL_R4_PATTERN = Pattern.compile("\\S*");
    private static final Pattern UUID_R4_PATTERN =
            Pattern.compile(
                    "urn:uuid:[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

    private static final Set<String> XHTML_ATTRIBUTES_ALLOWED_ON_ALL_ELEMENTS =
            Set.of("class", "dir", "id", "lang", "style", "title");
    private static final Set<String> XHTML_TABLE_ELEMENT_ALLOWED_ATTRIBUTES =
            Set.of("align", "char", "charoff", "valign");
    private static final Set<String> XHTML_COLUMN_ALLOWED_ATTRIBUTES =
            Stream.concat(
                            Set.of("span", "width").stream(),
                            XHTML_TABLE_ELEMENT_ALLOWED_ATTRIBUTES.stream())
                    .collect(Collectors.toUnmodifiableSet());
    private static final Map<String, Set<String>> XHTML_ELEMENT_TO_ATTRIBUTES_FOR_LINK_VALIDATION =
            Map.of("a", Set.of("href"), "img", Set.of("longdesc", "src"));

    static void validate(Object fieldObject, String fullFieldName, R4FhirType type) {
        if (!Flags.phrFhirPrimitiveTypeValidation()) {
            throw new UnsupportedOperationException(
                    "Validating FHIR primitive types is not supported.");
        }
        if (fieldObject == null) {
            throw new IllegalStateException(
                    "The fieldObject cannot be null in primitive kind field: " + fullFieldName);
        }
        populateR4PrimitiveIntegerTypeToMinValueMap();
        populateR4PrimitiveStringTypeToPatternMap();
        switch (type) {
            case R4_FHIR_TYPE_BOOLEAN:
                validateBooleanType(fieldObject, fullFieldName);
                break;
            case R4_FHIR_TYPE_DECIMAL:
                validateDecimalType(fieldObject, fullFieldName);
                break;
            case R4_FHIR_TYPE_INTEGER:
            case R4_FHIR_TYPE_POSITIVE_INT:
            case R4_FHIR_TYPE_UNSIGNED_INT:
                validateIntegerType(fieldObject, fullFieldName);
                validateIntegerValueRange(
                        (Integer) fieldObject,
                        sR4PrimitiveIntegerTypeToMinValueMap.get(type),
                        fullFieldName);
                break;
            case R4_FHIR_TYPE_BASE64_BINARY:
            case R4_FHIR_TYPE_CANONICAL:
            case R4_FHIR_TYPE_CODE:
            case R4_FHIR_TYPE_DATE:
            case R4_FHIR_TYPE_DATE_TIME:
            case R4_FHIR_TYPE_ID:
            case R4_FHIR_TYPE_INSTANT:
            case R4_FHIR_TYPE_MARKDOWN:
            case R4_FHIR_TYPE_OID:
            case R4_FHIR_TYPE_STRING:
            case R4_FHIR_TYPE_TIME:
            case R4_FHIR_TYPE_URI:
            case R4_FHIR_TYPE_URL:
            case R4_FHIR_TYPE_UUID:
                validateStringType(fieldObject, fullFieldName);
                validateStringValuePattern(
                        fieldObject.toString(),
                        getR4PrimitiveStringTypePattern(type),
                        fullFieldName);
                break;
            case R4_FHIR_TYPE_XHTML:
                validateStringType(fieldObject, fullFieldName);
                if (Flags.phrXhtmlValidation()) {
                    validateXhtmlString((String) fieldObject, fullFieldName);
                }
                break;
            default:
                throw new IllegalStateException(
                        "Type is not supported. Found unexpected type "
                                + type.name()
                                + " in primitive kind field: "
                                + fullFieldName);
        }
    }

    private static void validateBooleanType(Object fieldObject, String fullFieldName) {
        if (!(fieldObject instanceof Boolean)) {
            throw new IllegalArgumentException(
                    "Invalid resource structure. Found non boolean object in field: "
                            + fullFieldName);
        }
    }

    private static void validateDecimalType(Object fieldObject, String fullFieldName) {
        // According to the decimal regex and description from
        // https://hl7.org/fhir/R4/datatypes.html#decimal, the decimal data type allows values
        // without a decimal point, which means the valid values can then be parsed as an Integer,
        // Long, or Double. To make sure we don't reject any valid decimal types, we just check it's
        // an instance of "Number" here.
        if (!(fieldObject instanceof Number)) {
            throw new IllegalArgumentException(
                    "Invalid resource structure. Found non decimal object in field: "
                            + fullFieldName);
        }
    }

    private static void validateIntegerType(Object fieldObject, String fullFieldName) {
        if (!(fieldObject instanceof Integer)) {
            throw new IllegalArgumentException(
                    "Invalid resource structure. Found non integer object in field: "
                            + fullFieldName);
        }
    }

    private static void validateStringType(Object fieldObject, String fullFieldName) {
        if (!(fieldObject instanceof String)) {
            throw new IllegalArgumentException(
                    "Invalid resource structure. Found non string object in field: "
                            + fullFieldName);
        }
    }

    private static void validateIntegerValueRange(
            Integer value, @Nullable Integer min, String fullFieldName) {
        if (min != null && value < min) {
            throw new IllegalArgumentException(
                    "Found invalid field value in primitive field: "
                            + fullFieldName
                            + ". The value found is: "
                            + value);
        }
    }

    private static void validateStringValuePattern(
            String value, Pattern pattern, String fullFieldName) {
        Matcher matcher = pattern.matcher(value);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "Found invalid field value in primitive field: "
                            + fullFieldName
                            + ". The value found is: "
                            + value);
        }
    }

    private static void validateXhtmlString(String xhtml, String fullFieldName) {
        populateXhtmlElementToAttributesAllowlistMap();
        XmlPullParser parser = createXmlPullParserAndSetInput(xhtml);

        boolean alreadyProcessedRootElement = false;
        while (getNextTokenAndHandleException(parser, fullFieldName)
                != XmlPullParser.END_DOCUMENT) {
            int eventType;
            try {
                eventType = parser.getEventType();
            } catch (XmlPullParserException e) {
                throw new IllegalArgumentException(
                        "Failed to parse xhtml in field: " + fullFieldName);
            }
            switch (eventType) {
                case XmlPullParser.PROCESSING_INSTRUCTION:
                    throw new IllegalArgumentException(
                            "Found invalid xhtml containing processing instruction in field: "
                                    + fullFieldName);
                case XmlPullParser.DOCDECL:
                    throw new IllegalArgumentException(
                            "Found invalid xhtml containing DOCTYPE declaration in field: "
                                    + fullFieldName);
                case XmlPullParser.CDSECT:
                    throw new IllegalArgumentException(
                            "Found invalid xhtml containing CDATA section in field: "
                                    + fullFieldName);
                case XmlPullParser.START_TAG:
                    String elementName = parser.getName();
                    // The first START_TAG is the root element, which should be a div according to
                    // the FHIR spec.
                    if (!alreadyProcessedRootElement) {
                        if (!elementName.equals("div")) {
                            throw new IllegalArgumentException(
                                    "Found invalid xhtml in field: "
                                            + fullFieldName
                                            + ". Expected div as the root element");
                        }
                        alreadyProcessedRootElement = true;
                    } else if (parser.getDepth() == 1) {
                        throw new IllegalArgumentException(
                                "Found invalid xhtml with more than one root element in field: "
                                        + fullFieldName);
                    }
                    Set<String> allowedAttributes =
                            sXhtmlElementToAttributesAllowlistMap.get(elementName);
                    if (allowedAttributes == null) {
                        throw new IllegalArgumentException(
                                "Found invalid xhtml containing disallowed element "
                                        + elementName
                                        + " in field: "
                                        + fullFieldName);
                    }
                    for (int i = 0; i < parser.getAttributeCount(); i++) {
                        String attributeName = parser.getAttributeName(i);
                        if (!allowedAttributes.contains(attributeName)
                                && !XHTML_ATTRIBUTES_ALLOWED_ON_ALL_ELEMENTS.contains(
                                        attributeName)) {
                            throw new IllegalArgumentException(
                                    "Found invalid xhtml containing disallowed attribute "
                                            + elementName
                                            + "."
                                            + attributeName
                                            + " in field: "
                                            + fullFieldName);
                        }
                        if (requiresXhtmlLinkValidation(elementName, attributeName)) {
                            validateXhtmlLink(
                                    parser.getAttributeValue(i),
                                    elementName + "." + attributeName,
                                    fullFieldName);
                        }
                    }
                    break;
                default:
                    // Other event types can be ignored as they are mostly the html content,
                    // comments or END_TAG (which is covered by validating START_TAG).
            }
        }
        // After the end of the document has been reached the parsing depth should be 0. If not,
        // this means that there are still open tags left that have not been closed.
        if (parser.getDepth() != 0) {
            throw new IllegalArgumentException(
                    "Missing closing tag for xhtml element in field: " + fullFieldName);
        }
    }

    private static XmlPullParser createXmlPullParserAndSetInput(String xhtml) {
        XmlPullParser parser = Xml.newPullParser();
        try {
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
            // We don't allow DOCTYPE declarations so no need to process them
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false);
        } catch (XmlPullParserException e) {
            throw new IllegalStateException("Failed to set xml parsing feature");
        }

        try {
            parser.setInput(new StringReader(xhtml));
        } catch (XmlPullParserException e) {
            throw new IllegalStateException("Failed to set xml parsing input");
        }
        return parser;
    }

    private static boolean requiresXhtmlLinkValidation(String element, String attribute) {
        return XHTML_ELEMENT_TO_ATTRIBUTES_FOR_LINK_VALIDATION.containsKey(element)
                && XHTML_ELEMENT_TO_ATTRIBUTES_FOR_LINK_VALIDATION.get(element).contains(attribute);
    }

    private static void validateXhtmlLink(
            String value, String fullElementAttributeName, String fullFieldName) {
        URI parsedUri;
        try {
            parsedUri = new URI(value);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(
                    "Found invalid xhtml link uri in field: " + fullFieldName);
        }

        String scheme = parsedUri.getScheme();
        switch (scheme) {
            case null:
                // This means the URI is a relative URI, which is needed to for example refer to
                // the id of a contained resource (e.g. #observation1), but we disallow directory
                // traversal sequences.
                if (parsedUri.toString().contains("../")) {
                    throw new IllegalArgumentException(
                            "Found invalid xhtml link containing '../' in field: " + fullFieldName);
                }
                break;
            case "http":
            case "https":
            case "mailto":
            case "tel":
                // These schemes are allowed, so no action is needed
                break;
            case "data":
                // The data schema is only allowed for the img.src attribute
                if (!fullElementAttributeName.equals("img.src")) {
                    throw new IllegalArgumentException(
                            "Found invalid xhtml link due to disallowed data scheme in field: "
                                    + fullFieldName);
                }
                // Reject non-image and “image/svg+xml” types
                String mediaType =
                        extractLowerCaseMediaTypeFromDataUrlData(parsedUri.getSchemeSpecificPart());
                if (mediaType == null || !mediaType.startsWith("image/")) {
                    throw new IllegalArgumentException(
                            "Found invalid xhtml link due to missing or non-image media type in"
                                    + " data url in field: "
                                    + fullFieldName);
                }
                if (mediaType.equals("image/svg+xml")) {
                    throw new IllegalArgumentException(
                            "Found invalid xhtml link due to disallowed media type `image/svg+xml`"
                                    + " in data url in field: "
                                    + fullFieldName);
                }
                break;
            default:
                throw new IllegalArgumentException(
                        "Found invalid xhtml link due to disallowed "
                                + scheme
                                + " scheme in field: "
                                + fullFieldName);
        }
    }

    @Nullable
    private static String extractLowerCaseMediaTypeFromDataUrlData(String data) {
        // See https://datatracker.ietf.org/doc/html/rfc2397#section-3 for the data url format.
        String[] typeParts = data.split(",");
        if (typeParts.length == 0) {
            return null;
        }
        String mediaType = typeParts[0];

        String[] parameterParts = mediaType.split(";");
        if (parameterParts.length == 0) {
            return null;
        }
        String mediaTypeWithoutParameters = parameterParts[0];

        return mediaTypeWithoutParameters.isEmpty()
                ? null
                : mediaTypeWithoutParameters.toLowerCase(Locale.ROOT);
    }

    private static int getNextTokenAndHandleException(XmlPullParser parser, String fullFieldName) {
        try {
            return parser.nextToken();
        } catch (XmlPullParserException | IOException | RuntimeException e) {
            // We catch RuntimeException as well because the parser can throw it due to invalid
            // input.
            throw new IllegalArgumentException("Failed to parse xhtml in field: " + fullFieldName);
        }
    }

    private static Pattern getR4PrimitiveStringTypePattern(R4FhirType type) {
        populateR4PrimitiveStringTypeToPatternMap();

        Pattern pattern = sR4PrimitiveStringTypeToPatternMap.get(type);
        if (pattern != null) {
            return pattern;
        }

        throw new IllegalStateException(
                "Could not find the regex pattern for primitive string type " + type.name());
    }

    private static synchronized void populateR4PrimitiveIntegerTypeToMinValueMap() {
        if (!sR4PrimitiveIntegerTypeToMinValueMap.isEmpty()) {
            return;
        }
        sR4PrimitiveIntegerTypeToMinValueMap.put(R4_FHIR_TYPE_POSITIVE_INT, 1);
        sR4PrimitiveIntegerTypeToMinValueMap.put(R4_FHIR_TYPE_UNSIGNED_INT, 0);
    }

    private static synchronized void populateR4PrimitiveStringTypeToPatternMap() {
        if (!sR4PrimitiveStringTypeToPatternMap.isEmpty()) {
            return;
        }
        sR4PrimitiveStringTypeToPatternMap.put(
                R4_FHIR_TYPE_BASE64_BINARY, BASE64_BINARY_R4_PATTERN);
        sR4PrimitiveStringTypeToPatternMap.put(R4_FHIR_TYPE_CANONICAL, CANONICAL_R4_PATTERN);
        sR4PrimitiveStringTypeToPatternMap.put(R4_FHIR_TYPE_CODE, CODE_R4_PATTERN);
        sR4PrimitiveStringTypeToPatternMap.put(R4_FHIR_TYPE_DATE, DATE_R4_PATTERN);
        sR4PrimitiveStringTypeToPatternMap.put(R4_FHIR_TYPE_DATE_TIME, DATE_TIME_R4_PATTERN);
        sR4PrimitiveStringTypeToPatternMap.put(R4_FHIR_TYPE_ID, ID_R4_PATTERN);
        sR4PrimitiveStringTypeToPatternMap.put(R4_FHIR_TYPE_INSTANT, INSTANT_R4_PATTERN);
        sR4PrimitiveStringTypeToPatternMap.put(R4_FHIR_TYPE_MARKDOWN, MARKDOWN_R4_PATTERN);
        sR4PrimitiveStringTypeToPatternMap.put(R4_FHIR_TYPE_OID, OID_R4_PATTERN);
        sR4PrimitiveStringTypeToPatternMap.put(R4_FHIR_TYPE_STRING, STRING_R4_PATTERN);
        sR4PrimitiveStringTypeToPatternMap.put(R4_FHIR_TYPE_TIME, TIME_R4_PATTERN);
        sR4PrimitiveStringTypeToPatternMap.put(R4_FHIR_TYPE_URI, URI_R4_PATTERN);
        sR4PrimitiveStringTypeToPatternMap.put(R4_FHIR_TYPE_URL, URL_R4_PATTERN);
        sR4PrimitiveStringTypeToPatternMap.put(R4_FHIR_TYPE_UUID, UUID_R4_PATTERN);
    }

    private static synchronized void populateXhtmlElementToAttributesAllowlistMap() {
        if (!sXhtmlElementToAttributesAllowlistMap.isEmpty()) {
            return;
        }
        sXhtmlElementToAttributesAllowlistMap.put(
                "a",
                Set.of(
                        "accesskey",
                        "charset",
                        "href",
                        "hreflang",
                        "name",
                        "rel",
                        "rev",
                        "tabindex",
                        "target",
                        "type"));
        sXhtmlElementToAttributesAllowlistMap.put("abbr", Set.of());
        sXhtmlElementToAttributesAllowlistMap.put("acronym", Set.of());
        sXhtmlElementToAttributesAllowlistMap.put("b", Set.of());
        sXhtmlElementToAttributesAllowlistMap.put("bdo", Set.of());
        sXhtmlElementToAttributesAllowlistMap.put("big", Set.of());
        sXhtmlElementToAttributesAllowlistMap.put("blockquote", Set.of("cite"));
        sXhtmlElementToAttributesAllowlistMap.put("br", Set.of());
        sXhtmlElementToAttributesAllowlistMap.put("caption", Set.of());
        sXhtmlElementToAttributesAllowlistMap.put("cite", Set.of());
        sXhtmlElementToAttributesAllowlistMap.put("code", Set.of());
        sXhtmlElementToAttributesAllowlistMap.put("col", XHTML_COLUMN_ALLOWED_ATTRIBUTES);
        sXhtmlElementToAttributesAllowlistMap.put("colgroup", XHTML_COLUMN_ALLOWED_ATTRIBUTES);
        sXhtmlElementToAttributesAllowlistMap.put("dd", Set.of());
        sXhtmlElementToAttributesAllowlistMap.put("dfn", Set.of());
        sXhtmlElementToAttributesAllowlistMap.put("div", Set.of());
        sXhtmlElementToAttributesAllowlistMap.put("dl", Set.of());
        sXhtmlElementToAttributesAllowlistMap.put("dt", Set.of());
        sXhtmlElementToAttributesAllowlistMap.put("em", Set.of());
        sXhtmlElementToAttributesAllowlistMap.put("h1", Set.of());
        sXhtmlElementToAttributesAllowlistMap.put("h2", Set.of());
        sXhtmlElementToAttributesAllowlistMap.put("h3", Set.of());
        sXhtmlElementToAttributesAllowlistMap.put("h4", Set.of());
        sXhtmlElementToAttributesAllowlistMap.put("h5", Set.of());
        sXhtmlElementToAttributesAllowlistMap.put("h6", Set.of());
        sXhtmlElementToAttributesAllowlistMap.put("hr", Set.of());
        sXhtmlElementToAttributesAllowlistMap.put("i", Set.of());
        sXhtmlElementToAttributesAllowlistMap.put(
                "img", Set.of("alt", "height", "longdesc", "src", "width"));
        sXhtmlElementToAttributesAllowlistMap.put("kbd", Set.of());
        sXhtmlElementToAttributesAllowlistMap.put("li", Set.of());
        sXhtmlElementToAttributesAllowlistMap.put("ol", Set.of());
        sXhtmlElementToAttributesAllowlistMap.put("p", Set.of());
        sXhtmlElementToAttributesAllowlistMap.put("pre", Set.of());
        sXhtmlElementToAttributesAllowlistMap.put("q", Set.of("cite"));
        sXhtmlElementToAttributesAllowlistMap.put("samp", Set.of());
        sXhtmlElementToAttributesAllowlistMap.put("small", Set.of());
        sXhtmlElementToAttributesAllowlistMap.put("span", Set.of());
        sXhtmlElementToAttributesAllowlistMap.put("strong", Set.of());
        sXhtmlElementToAttributesAllowlistMap.put("sub", Set.of());
        sXhtmlElementToAttributesAllowlistMap.put("sup", Set.of());
        sXhtmlElementToAttributesAllowlistMap.put(
                "table",
                Set.of(
                        "border",
                        "cellpadding",
                        "cellspacing",
                        "frame",
                        "rules",
                        "summary",
                        "width"));
        sXhtmlElementToAttributesAllowlistMap.put("tbody", XHTML_TABLE_ELEMENT_ALLOWED_ATTRIBUTES);
        sXhtmlElementToAttributesAllowlistMap.put(
                "td",
                Set.of(
                        "abbr", "align", "axis", "char", "charoff", "colspan", "headers", "rowspan",
                        "scope", "valign"));
        sXhtmlElementToAttributesAllowlistMap.put("tfoot", XHTML_TABLE_ELEMENT_ALLOWED_ATTRIBUTES);
        sXhtmlElementToAttributesAllowlistMap.put(
                "th",
                Set.of(
                        "abbr", "align", "axis", "char", "charoff", "colspan", "headers", "rowspan",
                        "scope", "valign"));
        sXhtmlElementToAttributesAllowlistMap.put("thead", XHTML_TABLE_ELEMENT_ALLOWED_ATTRIBUTES);
        sXhtmlElementToAttributesAllowlistMap.put("tr", XHTML_TABLE_ELEMENT_ALLOWED_ATTRIBUTES);
        sXhtmlElementToAttributesAllowlistMap.put("tt", Set.of());
        sXhtmlElementToAttributesAllowlistMap.put("ul", Set.of());
        sXhtmlElementToAttributesAllowlistMap.put("var", Set.of());
    }
}
