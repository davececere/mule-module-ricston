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


    /*
     * called by the framework during rollback.
     * 
     * since we are doing the actual store() during the transaction begin (object.store()) call, we need to undo that here.
     * 
     * (non-Javadoc)
     * @see org.mule.util.xa.AbstractResourceManager#doRollback(org.mule.util.xa.AbstractTransactionContext)
     */
    @Override
    protected void doRollback(AbstractTransactionContext transactionContext) throws ResourceManagerException {
        Serializable key = ((ObjectStoreTransactionContext)transactionContext).getObject().getKey();

        try {
            if(getObjectStore().contains(key))
                getObjectStore().remove(key);
        } catch (ObjectStoreException e) {
            throw new ResourceManagerException(e);
        }     
        transactionContext.doRollback();
    }

    /*
     * called by framework at beginning of transaction, which is the same callstack as initial store() call on object store
     * (non-Javadoc)
     * @see org.mule.util.xa.AbstractResourceManager#doBegin(org.mule.util.xa.AbstractTransactionContext)
     */
    @Override
    protected void doBegin(AbstractTransactionContext context) {
    }

    /*
     * called by framework right before commit. Failure here dirties the thread so it never works again, don't let that happen.
     * 
     * For this reason, we do all of the work during objectstore.store() and/or begin. At this point we're really not doing anything to commit
     * (non-Javadoc)
     * @see org.mule.util.xa.AbstractResourceManager#doPrepare(org.mule.util.xa.AbstractTransactionContext)
     */
    @Override
    protected int doPrepare(AbstractTransactionContext context) {
        return XAResource.XA_OK;
    }

    /*
     * Since we do everything during objectStore.store() we don't need to do anything here
     * (non-Javadoc)
     * @see org.mule.util.xa.AbstractResourceManager#doCommit(org.mule.util.xa.AbstractTransactionContext)
     */
    @Override
    protected void doCommit(AbstractTransactionContext context) throws ResourceManagerException {
        context.doCommit();
    }

}
