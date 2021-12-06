/**
 * Copyright (c) 2012-2013 Reficio (TM) - Reestablish your software!. All Rights Reserved.
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.reficio.ws;

import java.util.HashSet;
import java.util.Set;
import javax.xml.namespace.QName;

/**
 * Specifies the context of the SOAP message generation.
 *
 * @author Tom Bujok
 * @since 1.0.0
 */
public class SoapContext {

    public final static SoapContext DEFAULT = SoapContext.builder().build();
    public final static SoapContext NO_CONTENT = SoapContext.builder().exampleContent(false).build();

    /**
     * Generates comments with type information in new requests
     */
    private final boolean typeComments;
    private final boolean valueComments;
    private final boolean exampleContent;
    private final boolean buildOptional;
    private final boolean alwaysBuildHeaders;
    private final boolean alwaysBuildEnvelope;
    private final boolean alwaysBuildBody;
    private final boolean bindingOperation;

    /*
     * A list of XML-Schema types and global elements in the form of name@namespace which
     * will be excluded when generating sample requests and responses and input forms.
     * By default the XML-Schema root element is added since it is quite common in .NET
     * services and generates a sample xml fragment of about 300 kb!.
     */
    private final Set<QName> excludedTypes;
    private final SoapMultiValuesProvider multiValuesProvider;

    /**
     * Constructor mainly for SpringFramework purposes, in any other case use the fluent builder interface;
     * #see builder() method
     *
     * @param exampleContent
     * @param typeComments
     * @param valueComments
     * @param buildOptional
     * @param alwaysBuildHeaders
     * @param excludedTypes
     */
    public SoapContext(final boolean exampleContent, final boolean typeComments, final boolean valueComments,
            final boolean buildOptional, final boolean alwaysBuildHeaders, final boolean alwaysBuildEnvelope, final boolean alwaysBuildBody,
            final boolean bindingOperation, final Set<QName> excludedTypes, final SoapMultiValuesProvider multiValuesProvider) {
        this.exampleContent = exampleContent;
        this.typeComments = typeComments;
        this.valueComments = valueComments;
        this.buildOptional = buildOptional;
        this.alwaysBuildHeaders = alwaysBuildHeaders;
        this.alwaysBuildEnvelope = alwaysBuildEnvelope;
        this.alwaysBuildBody = alwaysBuildBody;
        this.bindingOperation = bindingOperation;
        this.excludedTypes = new HashSet<>(excludedTypes);
        this.multiValuesProvider = multiValuesProvider;
    }

    /**
     * Constructor mainly for SpringFramework purposes, in any other case use the fluent builder interface;
     * #see builder() method
     *
     * @param exampleContent
     * @param typeComments
     * @param valueComments
     * @param buildOptional
     * @param alwaysBuildHeaders
     */
    public SoapContext(final boolean exampleContent, final boolean typeComments, final boolean valueComments,
            final boolean buildOptional, final boolean alwaysBuildHeaders, final boolean alwaysBuildEnvelope,
            final boolean alwaysBuildBody, final boolean bindingOperation) {
        this.exampleContent = exampleContent;
        this.typeComments = typeComments;
        this.valueComments = valueComments;
        this.buildOptional = buildOptional;
        this.alwaysBuildHeaders = alwaysBuildHeaders;
        this.alwaysBuildEnvelope = alwaysBuildEnvelope;
        this.alwaysBuildBody = alwaysBuildBody;
        this.bindingOperation = bindingOperation;
        this.excludedTypes = new HashSet<>();
        this.multiValuesProvider = null;
    }

    public boolean isBuildOptional() {
        return this.buildOptional;
    }

    public boolean isAlwaysBuildHeaders() {
        return this.alwaysBuildHeaders;
    }

    public boolean isAlwaysBuildBody() {
        return this.alwaysBuildBody;
    }

    public boolean isAlwaysBuildEnvelope() {
        return this.alwaysBuildEnvelope;
    }

    public boolean isBindingOperation() {
        return this.bindingOperation;
    }

    public boolean isExampleContent() {
        return this.exampleContent;
    }

    public boolean isTypeComments() {
        return this.typeComments;
    }

    public boolean isValueComments() {
        return this.valueComments;
    }

    public Set<QName> getExcludedTypes() {
        return new HashSet<>(this.excludedTypes);
    }

    public SoapMultiValuesProvider getMultiValuesProvider() {
        return this.multiValuesProvider;
    }

    public static ContextBuilder builder() {
        return new ContextBuilder();
    }

    public static class ContextBuilder {
        private boolean exampleContent = true;
        private boolean typeComments = false;
        private boolean valueComments = false;
        private boolean buildOptional = true;
        private boolean alwaysBuildHeaders = true;
        private boolean alwaysBuildEnvelope = true;
        private boolean alwaysBuildBody = true;
        private boolean bindingOperation = true;
        private Set<QName> excludedTypes = new HashSet<>();
        private SoapMultiValuesProvider multiValuesProvider = null;

        /**
         * Specifies if to generate example SOAP message content
         *
         * @param value
         * @return builder
         */
        public ContextBuilder exampleContent(final boolean value) {
            this.exampleContent = value;
            return this;
        }

        /**
         * Specifies if to generate SOAP message type comments
         *
         * @param value
         * @return builder
         */
        public ContextBuilder typeComments(final boolean value) {
            this.typeComments = value;
            return this;
        }

        /**
         * Specifies if to skip SOAP message comments
         *
         * @param value
         * @return builder
         */
        public ContextBuilder valueComments(final boolean value) {
            this.valueComments = value;
            return this;
        }

        /**
         * Specifies if to generate content for elements marked as optional
         *
         * @param value
         * @return builder
         */
        public ContextBuilder buildOptional(final boolean value) {
            this.buildOptional = value;
            return this;
        }

        /**
         * Specifies if to always build SOAP headers
         *
         * @param value
         * @return builder
         */
        public ContextBuilder alwaysBuildHeaders(final boolean value) {
            this.alwaysBuildHeaders = value;
            return this;
        }

        /**
         * Specifies if to always build SOAP envelope
         *
         * @return builder
         */
        public ContextBuilder alwaysBuildEnvelope(final boolean value) {
            this.alwaysBuildEnvelope = value;
            return this;
        }

        /**
         * Specifies if to always build SOAP body
         *
         * @return builder
         */
        public ContextBuilder alwaysBuildBody(final boolean value) {
            this.alwaysBuildBody = value;
            return this;
        }

        /**
         * Specifies if to include binding operation element
         *
         * @return builder
         */
        public ContextBuilder bindingOperation(final boolean value) {
            this.bindingOperation = value;
            return this;
        }

        /**
         * A list of XML-Schema types and global elements in the form of name@namespace which
         * will be excluded when generating sample requests and responses and input forms.
         * By default the XML-Schema root element is added since it is quite common in .NET
         * services and generates a sample xml fragment of about 300 kb!.
         *
         * @param excludedTypes
         * @return builder
         */
        public ContextBuilder excludedTypes(final Set<QName> excludedTypes) {
            this.excludedTypes = new HashSet<>(excludedTypes);
            return this;
        }

        public ContextBuilder multiValuesProvider(final SoapMultiValuesProvider multiValuesProvider) {
            this.multiValuesProvider = multiValuesProvider;
            return this;
        }

        /**
         * Builds populated context instance
         *
         * @return fully populated soap context
         */
        public SoapContext build() {
            return new SoapContext(this.exampleContent, this.typeComments, this.valueComments,
                    this.buildOptional, this.alwaysBuildHeaders, this.alwaysBuildEnvelope, this.alwaysBuildBody, this.bindingOperation,
                    this.excludedTypes,
                    this.multiValuesProvider);
        }
    }

}
