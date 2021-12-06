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
package org.reficio.ws.builder.core;

import com.google.common.base.Preconditions;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.wsdl.Binding;
import javax.wsdl.WSDLException;
import javax.xml.namespace.QName;
import org.apache.commons.lang3.StringUtils;
import org.reficio.ws.SoapBuilderException;
import org.reficio.ws.SoapContext;
import org.reficio.ws.builder.SoapBuilder;
import org.reficio.ws.builder.SoapBuilderFinder;
import org.reficio.ws.legacy.SoapLegacyFacade;

/**
 * @author Tom Bujok
 * @since 1.0.0
 */
public final class Wsdl {

    private final URL wsdlUrl;
    private final SoapLegacyFacade soapFacade;

    private Wsdl(final URL wsdlUrl) {
        try {
            this.wsdlUrl = wsdlUrl;
            this.soapFacade = new SoapLegacyFacade(wsdlUrl);
        } catch (final WSDLException e) {
            throw new SoapBuilderException(e);
        }
    }

    public static Wsdl parse(final URL wsdlUrl) {
        Preconditions.checkNotNull(wsdlUrl, "URL of the WSDL cannot be null");
        return new Wsdl(wsdlUrl);
    }

    public static Wsdl parse(final String wsdlUrl) {
        Preconditions.checkNotNull(wsdlUrl, "URL of the WSDL cannot be null");
        try {
            return new Wsdl(new URL(wsdlUrl));
        } catch (final MalformedURLException e) {
            throw new SoapBuilderException(e);
        }
    }

    public List<QName> getBindings() {
        return this.soapFacade.getBindingNames();
    }

    public SoapBuilderFinder binding() {
        return new SoapBuilderFinderImpl();
    }

    public URL saveWsdl(final File rootWsdl) {
        return this.soapFacade.saveWsdl(rootWsdl.getName(), rootWsdl.getParentFile());
    }

    public static URL saveWsdl(final URL wsdlUrl, final File rootWsdl) {
        return SoapLegacyFacade.saveWsdl(wsdlUrl, rootWsdl.getName(), rootWsdl.getParentFile());
    }

    public void printBindings() {
        System.out.println(this.wsdlUrl);
        for (final QName bindingName : this.soapFacade.getBindingNames()) {
            System.out.println("\t" + bindingName.toString());
        }
    }

    class SoapBuilderFinderImpl implements SoapBuilderFinder {



        SoapBuilderFinderImpl() {
        }

        private String namespaceURI;
        private String localPart;
        private String prefix;
        private QName service;
        private String port;

        @Override
        public SoapBuilderFinder name(final String name) {
            return name(QName.valueOf(name));
        }

        @Override
        public SoapBuilderFinder name(final QName name) {
            this.namespaceURI = name.getNamespaceURI();
            this.localPart = name.getLocalPart();
            this.prefix = name.getPrefix();
            return this;
        }

        @Override
        public SoapBuilderFinder namespaceURI(final String namespaceURI) {
            this.namespaceURI = namespaceURI;
            return this;
        }

        @Override
        public SoapBuilderFinder localPart(final String localPart) {
            this.localPart = localPart;
            return this;
        }

        @Override
        public SoapBuilderFinder prefix(final String prefix) {
            this.prefix = prefix;
            return this;
        }

        @Override
        public SoapBuilderFinder serviceAndPort(final QName service, final String port) {
            this.service = service;
            this.port =port;
            return this;
        }

        @Override
        public SoapBuilder find() {
            validate();
            return getBuilder(getBindingName(), SoapContext.DEFAULT);
        }

        @Override
        public SoapBuilder find(final SoapContext context) {
            validate();
            return getBuilder(getBindingName(), context);
        }

        private QName getBindingName() {
            final List<QName> result = new ArrayList<>();
            for (final QName binding : Wsdl.this.soapFacade.getBindingNames()) {
                if (binding.getLocalPart().equals(this.localPart)) {
                    if (this.namespaceURI != null) {
                        if (!binding.getNamespaceURI().equals(this.namespaceURI)) {
                            continue;
                        }
                    }
                    if (this.prefix != null) {
                        if (!binding.getPrefix().equals(this.prefix)) {
                            continue;
                        }
                    }
                    result.add(binding);
                }
            }
            if(result.isEmpty()){
                if (this.service != null && this.port != null) {
                    Wsdl.this.soapFacade.getServices().stream()
                            .filter(s -> s.getQName().equals(this.service))
                            .findFirst()
                            .map(s -> s.getPort(this.port).getBinding().getQName())
                    .ifPresent(result::add);
                }
            }

            if (result.isEmpty()) {
                throw new SoapBuilderException("Binding not found");
            }
            if (result.size() > 1) {
                throw new SoapBuilderException("Found more than one binding " + result);
            }
            return result.iterator().next();
        }

        private void validate() {
            if (StringUtils.isBlank(this.localPart) && (Objects.isNull(this.service) || StringUtils.isBlank(this.port))) {
                throw new SoapBuilderException("Specify at least localPart of the binding's QName");
            }
        }
    }

    public SoapBuilder getBuilder(final String bindingName) {
        return getBuilder(bindingName, SoapContext.DEFAULT);
    }

    public SoapBuilder getBuilder(final String bindingName, final SoapContext context) {
        Preconditions.checkNotNull(bindingName, "BindingName cannot be null");
        return getBuilder(QName.valueOf(bindingName), context);
    }

    public SoapBuilder getBuilder(final QName bindingName) {
        return getBuilder(bindingName, SoapContext.DEFAULT);
    }

    public SoapBuilder getBuilder(final QName bindingName, final SoapContext context) {
        Preconditions.checkNotNull(bindingName, "BindingName cannot be null");
        Preconditions.checkNotNull(context, "SoapContext cannot be null");
        final Binding binding = this.soapFacade.getBindingByName(bindingName);
        return new SoapBuilderImpl(this.soapFacade, binding, context);
    }

}
