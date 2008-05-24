/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.server.core.interceptor.context;


import java.util.HashSet;
import java.util.Set;

import javax.naming.NamingException;
import javax.naming.directory.SearchControls;

import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.filter.SearchScope;
import org.apache.directory.shared.ldap.message.AliasDerefMode;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.util.ArrayUtils;

import static org.apache.directory.shared.ldap.filter.SearchScope.ONELEVEL;


/**
 * A context used for search related operations and used by all 
 * the Interceptors.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public abstract class SearchingOperationContext extends AbstractOperationContext
{
    private AliasDerefMode aliasDerefMode = AliasDerefMode.DEREF_ALWAYS;

    private long sizeLimit = 0;
    
    private int timeLimit = 0;
    
    private SearchScope scope = ONELEVEL;

    private boolean allOperationalAttributes = false;
    
    private boolean allUserAttributes = false;
    
    private boolean noAttributes = false;
    
    private Set<AttributeType> returningAttributes; 
    
    private boolean abandoned = false;
    
    
    /**
     * Creates a new instance of ListOperationContext.
     */
    public SearchingOperationContext( Registries registries )
    {
        super( registries );
    }


    /**
     * Creates a new instance of ListOperationContext.
     *
     * @param dn The DN to get the suffix from
     */
    public SearchingOperationContext( Registries registries, LdapDN dn )
    {
        super( registries, dn );
    }


    /**
     * Creates a new instance of ListOperationContext.
     *
     * @param dn The DN to get the suffix from
     * @param aliasDerefMode the alias dereferencing mode to use
     */
    public SearchingOperationContext( Registries registries, LdapDN dn, AliasDerefMode aliasDerefMode )
    {
        super( registries, dn );
        this.aliasDerefMode = aliasDerefMode;
    }

    
    /**
     * Creates a new instance of ListOperationContext.
     *
     * @param dn The DN to get the suffix from
     * @param aliasDerefMode the alias dereferencing mode to use
     * @throws NamingException 
     */
    public SearchingOperationContext( Registries registries, LdapDN dn, AliasDerefMode aliasDerefMode, 
        SearchControls searchControls ) throws NamingException
    {
        super( registries, dn );
        this.aliasDerefMode = aliasDerefMode;
        this.scope = SearchScope.getSearchScope( searchControls );
        this.timeLimit = searchControls.getTimeLimit();
        this.sizeLimit = searchControls.getCountLimit();
        
        if ( searchControls.getReturningAttributes() != null )
        {
            returningAttributes = new HashSet<AttributeType>();
            for ( String returnAttribute : searchControls.getReturningAttributes() )
            {
                if ( returnAttribute.equals( SchemaConstants.NO_ATTRIBUTE ) )
                {
                    noAttributes = true;
                    continue;
                }
                
                if ( returnAttribute.equals( SchemaConstants.ALL_OPERATIONAL_ATTRIBUTES ) )
                {
                    allOperationalAttributes = true;
                    continue;
                }
                
                if ( returnAttribute.equals( SchemaConstants.ALL_USER_ATTRIBUTES ) )
                {
                    allUserAttributes = true;
                    continue;
                }
                
                returningAttributes.add( registries.getAttributeTypeRegistry().lookup( returnAttribute ) );
            }
        }
    }

    
    
    
    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return "ListOperationContext with DN '" + getDn().getUpName() + "'";
    }

    
    public AliasDerefMode getAliasDerefMode()
    {
        return aliasDerefMode;
    }


    /**
     * @param sizeLimit the sizeLimit to set
     */
    public void setSizeLimit( long sizeLimit )
    {
        this.sizeLimit = sizeLimit;
    }


    /**
     * @return the sizeLimit
     */
    public long getSizeLimit()
    {
        return sizeLimit;
    }


    /**
     * @param timeLimit the timeLimit to set
     */
    public void setTimeLimit( int timeLimit )
    {
        this.timeLimit = timeLimit;
    }


    /**
     * @return the timeLimit
     */
    public int getTimeLimit()
    {
        return timeLimit;
    }


    /**
     * @param scope the scope to set
     */
    public void setScope( SearchScope scope )
    {
        this.scope = scope;
    }


    /**
     * @return the scope
     */
    public SearchScope getScope()
    {
        return scope;
    }


    /**
     * @param allOperationalAttributes the allOperationalAttributes to set
     */
    public void setAllOperationalAttributes( boolean allOperationalAttribute )
    {
        this.allOperationalAttributes = allOperationalAttribute;
    }


    /**
     * @return the allOperationalAttributes
     */
    public boolean isAllOperationalAttributes()
    {
        return allOperationalAttributes;
    }


    /**
     * @param allUserAttributes the allUserAttributes to set
     */
    public void setAllUserAttributes( boolean allUserAttributes )
    {
        this.allUserAttributes = allUserAttributes;
    }


    /**
     * @return the allUserAttributes
     */
    public boolean isAllUserAttributes()
    {
        return allUserAttributes;
    }


    /**
     * @param noAttributes the noAttributes to set
     */
    public void setNoAttributes( boolean noAttributes )
    {
        this.noAttributes = noAttributes;
    }


    /**
     * @return the noAttributes
     */
    public boolean isNoAttributes()
    {
        return noAttributes;
    }


    /**
     * @param returningAttributes the returningAttributes to set
     */
    public void setReturningAttributes( Set<AttributeType> returningAttributes )
    {
        this.returningAttributes = returningAttributes;
    }


    /**
     * @return the returningAttributes
     */
    public Set<AttributeType> getReturningAttributes()
    {
        return returningAttributes;
    }

    
    /**
     * Creates a new SearchControls object populated with the parameters 
     * contained in this SearchOperationContext in normalized form.
     *
     * @return a new SearchControls object
     */
    public SearchControls getSearchControls()
    {
        return getSearchControls( false );
    }
    
    
    /**
     * Creates a new SearchControls object populated with the parameters 
     * contained in this SearchOperationContext.
     *
     * @param denormalized true if attribute values are <b>not</b> normalized
     * @return a new SearchControls object
     */
    public SearchControls getSearchControls( boolean denormalized )
    {
        SearchControls controls = new SearchControls();
        controls.setCountLimit( sizeLimit );
        controls.setSearchScope( scope.getJndiScope() );
        controls.setTimeLimit( timeLimit );

        Set<String> allReturningAttributes = new HashSet<String>();
        
        if ( noAttributes )
        {
            allReturningAttributes.add( SchemaConstants.NO_ATTRIBUTE );
        }
        
        if ( allUserAttributes )
        {
            allReturningAttributes.add( SchemaConstants.ALL_USER_ATTRIBUTES );
        }
        
        if ( allOperationalAttributes )
        {
            allReturningAttributes.add( SchemaConstants.ALL_OPERATIONAL_ATTRIBUTES );
        }
        
        if ( returningAttributes != null )
        {
            for ( AttributeType at : returningAttributes )
            {
                if ( denormalized )
                {
                    allReturningAttributes.add( at.getName() );
                }
                else
                {
                    allReturningAttributes.add( at.getOid() );
                }
            }
        }
        
        controls.setReturningAttributes( allReturningAttributes.toArray( ArrayUtils.EMPTY_STRING_ARRAY ) );
        return controls;
    }


    /**
     * @param abandoned the abandoned to set
     */
    public void setAbandoned( boolean abandoned )
    {
        this.abandoned = abandoned;
    }


    /**
     * @return the abandoned
     */
    public boolean isAbandoned()
    {
        return abandoned;
    }
}
