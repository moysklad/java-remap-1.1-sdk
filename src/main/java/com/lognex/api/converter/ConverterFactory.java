package com.lognex.api.converter;

import com.lognex.api.converter.base.AbstractEntityConverter;
import com.lognex.api.converter.base.Converter;
import com.lognex.api.converter.base.PositionConverter;
import com.lognex.api.converter.base.ShipmentOutPositionConverter;
import com.lognex.api.converter.document.DemandConverter;
import com.lognex.api.converter.document.PaymentInConverter;
import com.lognex.api.converter.entity.*;
import com.lognex.api.model.base.AbstractEntity;
import com.lognex.api.model.base.ShipmentOutPosition;
import com.lognex.api.model.document.Demand;
import com.lognex.api.model.document.PaymentIn;
import com.lognex.api.model.entity.*;

import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings("unchecked")
public final class ConverterFactory {

    private static final Map<Class<? extends AbstractEntity>, Class<? extends AbstractEntityConverter>> converters;

    static {
        converters = new LinkedHashMap<>();
        converters.put(AgentAccount.class, AgentAccountConverter.class);
        converters.put(Contract.class, ContractConverter.class);
        converters.put(Counterparty.class, CounterpartyConverter.class);
        converters.put(Currency.class, CurrencyConverter.class);
        converters.put(Project.class, ProjectConverter.class);
        converters.put(Service.class, ServiceConverter.class);
        converters.put(Store.class, StoreConverter.class);

        converters.put(Demand.class, DemandConverter.class);
        converters.put(PaymentIn.class, PaymentInConverter.class);

        converters.put(ShipmentOutPosition.class, ShipmentOutPositionConverter.class);
    }

    private ConverterFactory() {
    }

    public static Converter<? extends AbstractEntity> getConverter(Class<? extends AbstractEntity> clazz){
        Class<? extends AbstractEntityConverter> converterClass = converters.get(clazz);
        try {
            return (Converter<? extends AbstractEntity>) converterClass.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
