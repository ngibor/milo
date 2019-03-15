/*
 * Copyright (c) 2019 the Eclipse Milo Authors
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.milo.opcua.sdk.server.nodes.factories;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.ObjectTypeManager;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.UaNodeManager;
import org.eclipse.milo.opcua.sdk.server.VariableTypeManager;
import org.eclipse.milo.opcua.sdk.server.api.AddressSpaceManager;
import org.eclipse.milo.opcua.sdk.server.api.NodeManager;
import org.eclipse.milo.opcua.sdk.server.model.nodes.objects.BaseEventNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

public class EventFactory {

    private final OpcUaServer server;

    private final ObjectTypeManager objectTypeManager;
    private final VariableTypeManager variableTypeManager;

    public EventFactory(
        UaNodeContext context,
        ObjectTypeManager objectTypeManager,
        VariableTypeManager variableTypeManager) {

        this.objectTypeManager = objectTypeManager;
        this.variableTypeManager = variableTypeManager;

        server = context.getServer();
    }

    public BaseEventNode createEvent(
        NodeId nodeId,
        NodeId typeDefinitionId) throws UaException {

        NodeFactory nodeFactory = new NodeFactory(
            new EventNodeContext(server),
            objectTypeManager,
            variableTypeManager
        );

        return (BaseEventNode) nodeFactory.createNode(nodeId, typeDefinitionId, true);
    }

    private static class EventNodeContext implements UaNodeContext {

        private final UaNodeManager nodeManager;

        private final OpcUaServer server;

        EventNodeContext(OpcUaServer server) {
            this.server = server;

            nodeManager = new EventNodeManager(server.getAddressSpaceManager());
        }

        @Override
        public OpcUaServer getServer() {
            return server;
        }

        @Override
        public Optional<NodeManager<UaNode>> getNodeManager(NodeId nodeId) {
            return Optional.of(nodeManager);
        }

    }

    private static class EventNodeManager extends UaNodeManager {

        private final AddressSpaceManager addressSpaceManager;

        private EventNodeManager(AddressSpaceManager addressSpaceManager) {
            this.addressSpaceManager = addressSpaceManager;
        }

        @Override
        public boolean containsNode(NodeId nodeId) {
            return super.containsNode(nodeId) ||
                addressSpaceManager.getNodeManager(nodeId)
                    .map(n -> n.containsNode(nodeId))
                    .orElse(false);
        }

        @Override
        public Optional<UaNode> getNode(NodeId nodeId) {
            Optional<UaNode> node = super.getNode(nodeId);

            if (node.isPresent()) {
                return node;
            } else {
                return addressSpaceManager.getNodeManager(nodeId)
                    .flatMap(n -> n.getNode(nodeId));
            }
        }

        @Override
        public List<Reference> getReferences(NodeId nodeId) {
            if (super.containsNode(nodeId)) {
                return super.getReferences(nodeId);
            } else {
                return addressSpaceManager.getNodeManager(nodeId)
                    .map(n -> n.getReferences(nodeId))
                    .orElse(Collections.emptyList());
            }
        }

    }

}
