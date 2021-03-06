package com.lognex.api.clients.documents;

import com.lognex.api.LognexApi;
import com.lognex.api.clients.ApiClient;
import com.lognex.api.clients.endpoints.DocumentMetadataEndpoint;
import com.lognex.api.clients.endpoints.GetListEndpoint;
import com.lognex.api.clients.endpoints.PostEndpoint;
import com.lognex.api.entities.MetaEntity;
import com.lognex.api.entities.documents.CustomerOrderDocumentEntity;
import com.lognex.api.responses.metadata.MetadataAttributeSharedStatesResponse;

public final class DocumentCustomerOrderClient
        extends ApiClient
        implements
        GetListEndpoint<CustomerOrderDocumentEntity>,
        PostEndpoint<CustomerOrderDocumentEntity>,
        DocumentMetadataEndpoint<MetadataAttributeSharedStatesResponse> {

    public DocumentCustomerOrderClient(LognexApi api) {
        super(api, "/entity/customerorder/");
    }

    @Override
    public Class<? extends MetaEntity> entityClass() {
        return CustomerOrderDocumentEntity.class;
    }

    @Override
    public Class<? extends MetaEntity> metaEntityClass() {
        return MetadataAttributeSharedStatesResponse.class;
    }
}
