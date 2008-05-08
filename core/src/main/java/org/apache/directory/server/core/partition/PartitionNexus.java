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
package org.apache.directory.server.core.partition;


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.naming.InvalidNameException;
import javax.naming.NamingException;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.interceptor.context.AddContextPartitionOperationContext;
import org.apache.directory.server.core.interceptor.context.CompareOperationContext;
import org.apache.directory.server.core.interceptor.context.GetMatchedNameOperationContext;
import org.apache.directory.server.core.interceptor.context.GetRootDSEOperationContext;
import org.apache.directory.server.core.interceptor.context.GetSuffixOperationContext;
import org.apache.directory.server.core.interceptor.context.ListSuffixOperationContext;
import org.apache.directory.server.core.interceptor.context.RemoveContextPartitionOperationContext;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.NoOpNormalizer;
import org.apache.directory.shared.ldap.schema.OidNormalizer;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * A root {@link Partition} that contains all other partitions, and
 * routes all operations to the child partition that matches to its base suffixes.
 * It also provides some extended operations such as accessing rootDSE and
 * listing base suffixes.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public abstract class PartitionNexus implements Partition
{
    /** the admin super user uid */
    public static final String ADMIN_UID = "admin";
    
    /** the initial admin passwd set on startup */
    public static final String ADMIN_PASSWORD_STRING = "secret";
    public static final byte[] ADMIN_PASSWORD_BYTES = StringTools.getBytesUtf8( ADMIN_PASSWORD_STRING );
    
   
    /**
     * Gets the DN for the admin user.
     * 
     * @return the admin user DN
     */
    public static final LdapDN getAdminName()
    {
        LdapDN adminDn = null;

        try
        {
            adminDn = new LdapDN( ServerDNConstants.ADMIN_SYSTEM_DN );
        }
        catch ( NamingException e )
        {
            throw new InternalError();
        }
        
        try
        {
        	Map<String, OidNormalizer> oidsMap = new HashMap<String, OidNormalizer>();
        	
        	oidsMap.put( SchemaConstants.UID_AT, new OidNormalizer( SchemaConstants.UID_AT_OID, new NoOpNormalizer() ) );
        	oidsMap.put( SchemaConstants.USER_ID_AT, new OidNormalizer( SchemaConstants.UID_AT_OID, new NoOpNormalizer() ) );
        	oidsMap.put( SchemaConstants.UID_AT_OID, new OidNormalizer( SchemaConstants.UID_AT_OID, new NoOpNormalizer() ) );
        	
        	oidsMap.put( SchemaConstants.OU_AT, new OidNormalizer( SchemaConstants.OU_AT_OID, new NoOpNormalizer()  ) );
        	oidsMap.put( SchemaConstants.ORGANIZATIONAL_UNIT_NAME_AT, new OidNormalizer( SchemaConstants.OU_AT_OID, new NoOpNormalizer()  ) );
        	oidsMap.put( SchemaConstants.OU_AT_OID, new OidNormalizer( SchemaConstants.OU_AT_OID, new NoOpNormalizer()  ) );

            adminDn.normalize( oidsMap );
        }
        catch ( InvalidNameException ine )
        {
            // Nothing we can do ...
        }
        catch ( NamingException ne )
        {
            // Nothing we can do ...
        }

        return adminDn;
    }


    /**
     * Gets the DN for the base entry under which all groups reside.
     * A new Name instance is created and returned every time.
     * @return the groups base DN
     */
    public static final LdapDN getGroupsBaseName()
    {
        LdapDN groupsBaseDn = null;

        try
        {
            groupsBaseDn = new LdapDN( ServerDNConstants.GROUPS_SYSTEM_DN );
        }
        catch ( NamingException e )
        {
            throw new InternalError();
        }

        return groupsBaseDn;
    }


    /**
     * Gets the DN for the base entry under which all non-admin users reside.
     * A new Name instance is created and returned every time.
     * @return the users base DN
     */
    public static final LdapDN getUsersBaseName()
    {
        LdapDN usersBaseDn = null;

        try
        {
            usersBaseDn = new LdapDN( ServerDNConstants.USERS_SYSTEM_DN );
        }
        catch ( NamingException e )
        {
            throw new InternalError();
        }

        return usersBaseDn;
    }


    /**
     * Gets the LdapContext associated with the calling thread.
     * 
     * @return The LdapContext associated with the thread of execution or null
     * if no context is associated with the calling thread.
     */
    public abstract LdapContext getLdapContext();


    /**
     * Get's the RootDSE entry for the DSA.
     *
     * @return the attributes of the RootDSE
     */
    public abstract ServerEntry getRootDSE( GetRootDSEOperationContext opContext ) throws NamingException;


    /**
     * Performs a comparison check to see if an attribute of an entry has
     * a specified value.
     *
     * @param compareContext the context used to compare
     * @return true if the entry contains an attribute with the value, false otherwise
     * @throws NamingException if there is a problem accessing the entry and its values
     */
    public abstract boolean compare( CompareOperationContext compareContext ) throws NamingException;


    public abstract void addContextPartition( AddContextPartitionOperationContext opContext ) throws NamingException;


    public abstract void removeContextPartition( RemoveContextPartitionOperationContext opContext ) throws NamingException;


    public abstract Partition getSystemPartition();


    /**
     * Get's the partition corresponding to a distinguished name.  This 
     * name need not be the name of the partition suffix.  When used in 
     * conjunction with get suffix this can properly find the partition 
     * associated with the DN.  Make sure to use the normalized DN.
     * 
     * @param dn the normalized distinguished name to get a partition for
     * @return the partition containing the entry represented by the dn
     * @throws NamingException if there is no partition for the dn
     */
    public abstract Partition getPartition( LdapDN dn ) throws NamingException;


    /**
     * Gets the most significant Dn that exists within the server for any Dn.
     *
     * @param getMatchedNameContext the context containing the  distinguished name 
     * to use for matching.
     * @return a distinguished name representing the matching portion of dn,
     * as originally provided by the user on creation of the matched entry or 
     * the empty string distinguished name if no match was found.
     * @throws NamingException if there are any problems
     */
    public abstract LdapDN getMatchedName ( GetMatchedNameOperationContext getMatchedNameContext ) throws NamingException;


    /**
     * Gets the distinguished name of the suffix that would hold an entry with
     * the supplied distinguished name parameter.  If the DN argument does not
     * fall under a partition suffix then the empty string Dn is returned.
     *
     * @param suffixContext the Context containing normalized distinguished
     * name to use for finding a suffix.
     * @return the suffix portion of dn, or the valid empty string Dn if no
     * naming context was found for dn.
     * @throws NamingException if there are any problems
     */
    public abstract LdapDN getSuffix ( GetSuffixOperationContext suffixContext ) throws NamingException;


    /**
     * Gets an iteration over the Name suffixes of the partitions managed by this
     * {@link PartitionNexus}.
     *
     * @return Iteration over ContextPartition suffix names as Names.
     * @throws NamingException if there are any problems
     */
    public abstract Iterator<String> listSuffixes( ListSuffixOperationContext opContext ) throws NamingException;


    /**
     * Adds a set of supportedExtension (OID Strings) to the RootDSE.
     * 
     * @param extensionOids a set of OID strings to add to the supportedExtension 
     * attribute in the RootDSE
     */
    public abstract void registerSupportedExtensions( Set<String> extensionOids ) throws NamingException;


    public abstract void registerSupportedSaslMechanisms( Set<String> strings ) throws NamingException;
}
