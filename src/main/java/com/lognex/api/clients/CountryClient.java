package com.lognex.api.clients;

import com.lognex.api.LognexApi;
import com.lognex.api.clients.endpoints.*;
import com.lognex.api.entities.CountryEntity;
import com.lognex.api.entities.MetaEntity;

public final class CountryClient
        extends ApiClient
        implements
        GetListEndpoint<CountryEntity>,
        PostEndpoint<CountryEntity>,
        DeleteByIdEndpoint,
        GetByIdEndpoint<CountryEntity> {

    public CountryClient(LognexApi api) {
        super(api, "/entity/country/");
    }

    @Override
    public Class<? extends MetaEntity> entityClass() {
        return CountryEntity.class;
    }
}
