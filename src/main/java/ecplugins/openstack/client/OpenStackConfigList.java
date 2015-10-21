/**
 *  Copyright 2015 Electric Cloud, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

// OpenStackConfigList.java --
//
// OpenStackConfigList.java is part of ElectricCommander.
//
// Copyright (c) 2005-2010 Electric Cloud, Inc.
// All rights reserved.
//

package ecplugins.openstack.client;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.XMLParser;

import static com.electriccloud.commander.gwt.client.util.XmlUtil.getNodeByName;
import static com.electriccloud.commander.gwt.client.util.XmlUtil.getNodeValueByName;
import static com.electriccloud.commander.gwt.client.util.XmlUtil.getNodesByName;

public class OpenStackConfigList
{

    //~ Instance fields --------------------------------------------------------

    private final Map<String, OpenStackConfigInfo> m_configInfo =
        new TreeMap<String, OpenStackConfigInfo>();

    //~ Methods ----------------------------------------------------------------

    public void addConfig(
            String configName,
            String configServer)
    {
        m_configInfo.put(configName, new OpenStackConfigInfo(configServer));
    }

    public String parseResponse(String cgiResponse)
    {
        Document document     = XMLParser.parse(cgiResponse);
        Node     responseNode = getNodeByName(document, "response");
        String   error        = getNodeValueByName(responseNode, "error");

        if (error != null && !error.isEmpty()) {
            return error;
        }

        Node       configListNode = getNodeByName(responseNode, "cfgs");
        List<Node> configNodes    = getNodesByName(configListNode, "cfg");

        for (Node configNode : configNodes) {
            String configName   = getNodeValueByName(configNode, "name");
            String configServer = getNodeValueByName(configNode, "url");

            addConfig(configName, configServer);
        }

        return null;
    }

    public void populateConfigListBox(ListBox lb)
    {

        for (String configName : m_configInfo.keySet()) {
            lb.addItem(configName);
        }
    }

    public Set<String> getConfigNames()
    {
        return m_configInfo.keySet();
    }

    public String getConfigServer(String configName)
    {
        return m_configInfo.get(configName).m_server;
    }

    public String getEditorDefinition(String configName)
    {
        return "EC-OpenStack";
    }

    public boolean isEmpty()
    {
        return m_configInfo.isEmpty();
    }

    public void setEditorDefinition(
            String configServer,
            String editorDefiniton)
    {
    }

    //~ Inner Classes ----------------------------------------------------------

    private class OpenStackConfigInfo
    {

        //~ Instance fields ----------------------------------------------------

        private String m_server;

        //~ Constructors -------------------------------------------------------

        public OpenStackConfigInfo(String server)
        {
            m_server = server;
        }
    }
}
