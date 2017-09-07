package com.lognex.api.endpoint;

import com.lognex.api.model.base.AbstractEntity;
import com.lognex.api.model.document.PaymentIn;

import javax.ejb.Stateless;

@Stateless
public class DocumentEndpoint {

    public AbstractEntity readDocument(String queryString) {
        return new PaymentIn();
    }
}