/*
 * Copyright (c) Ricston Ltd.  All rights reserved.  http://www.ricston.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ricston.objectstore;

import org.apache.log4j.Logger;
import org.mule.api.MuleContext;
import org.mule.api.store.*;
import org.mule.util.xa.AbstractTransactionContext;
import org.mule.util.xa.AbstractXAResourceManager;
import org.mule.util.xa.ResourceManagerException;
import org.mule.util.xa.ResourceManagerSystemException;

import java.io.*;

import javax.transaction.xa.XAResource;

public class ObjectStoreXAResourceManager extends AbstractXAResourceManager {

    private TransactionAwareObjectStore objectStore;
    private MuleContext muleContext;
    private static Logger logger = Logger.getLogger("lifecycle");


    public MuleContext getMuleContext() {
        return muleContext;
    }

    public void setMuleContext(MuleContext muleContext) {
        this.muleContext = muleContext;
    }

    public TransactionAwareObjectStore getObjectStore() {
        return objectStore;
    }

    public ObjectStoreXAResourceManager(TransactionAwareObjectStore objectStore, MuleContext muleContext) throws ObjectStoreException {
        super();
        this.objectStore = objectStore;
        this.muleContext = muleContext;
    }

    @Override
    protected AbstractTransactionContext createTransactionContext(Object session) {
        return new ObjectStoreTransactionContext(((ObjectStoreXASession) session).getObject());
    }


    @Override
    protected void doRollback(AbstractTransactionContext transactionContext) throws ResourceManagerException {
        logger.debug("rm rollback");
        Serializable key = ((ObjectStoreTransactionContext)transactionContext).getObject().getKey();

        try {
            if(getObjectStore().contains(key))
                getObjectStore().remove(key);
        } catch (ObjectStoreException e) {
            throw new ResourceManagerException(e);
        }     
        transactionContext.doRollback();
    }

    @Override
    protected void doBegin(AbstractTransactionContext context) {
        logger.debug("rm begin");
    }

    @Override
    protected int doPrepare(AbstractTransactionContext context) {
        logger.debug("rm prepare");
        return XAResource.XA_OK;
    }

    @Override
    protected void doCommit(AbstractTransactionContext context) throws ResourceManagerException {
        logger.debug("rm commit");
        context.doCommit();
    }

}
