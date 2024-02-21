package org.openelisglobal.hibernate.resources;

import java.io.Serializable;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.spi.JavaTypeBasicAdaptor;
import org.hibernate.type.descriptor.jdbc.NumericJdbcType;
import org.hibernate.type.internal.NamedBasicTypeImpl;

public class StringSequenceGenerator extends SequenceStyleGenerator {
    private String numberFormat = "%d";

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object object) throws HibernateException {
        Long id = (Long) super.generate(session, object);
        return String.format(numberFormat, id);
    }

    @Override
    public void configure(Type type, Properties params, ServiceRegistry dialect) throws MappingException {
        params.setProperty(INCREMENT_PARAM, "1");
        super.configure(new NamedBasicTypeImpl<>(new JavaTypeBasicAdaptor<>(Long.class),
                NumericJdbcType.INSTANCE, "long"), params, dialect);
                
    }

}
