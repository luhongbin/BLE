package no.nordicsemi.android.meshprovisioner;

import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import androidx.annotation.NonNull;
import no.nordicsemi.android.meshprovisioner.transport.Element;
import no.nordicsemi.android.meshprovisioner.transport.ProvisionedMeshNode;
import no.nordicsemi.android.meshprovisioner.utils.MeshAddress;
import no.nordicsemi.android.meshprovisioner.utils.MeshParserUtils;

public final class MeshNetworkDeserializer implements JsonSerializer<MeshNetwork>, JsonDeserializer<MeshNetwork> {
    private static final String TAG = MeshNetworkDeserializer.class.getSimpleName();

    @Override
    public MeshNetwork deserialize(final JsonElement json,
                                   final Type typeOfT,
                                   final JsonDeserializationContext context) throws JsonParseException {

        final JsonObject jsonObject = json.getAsJsonObject();
        if (!isValidMeshObject(jsonObject))
            throw new JsonSyntaxException("Invalid Mesh Provisioning/Configuration Database, " +
                    "Mesh Network must follow the Mesh Provisioning/Configuration Database format.");

        final String uuid = jsonObject.get("meshUUID").getAsString();
        final String meshUuid = MeshParserUtils.formatUuid(uuid);
        final MeshNetwork network = new MeshNetwork(meshUuid == null ? uuid : meshUuid);

        network.schema = jsonObject.get("$schema").getAsString();
        network.id = jsonObject.get("id").getAsString();
        network.version = jsonObject.get("version").getAsString();
        network.meshName = jsonObject.get("meshName").getAsString();

        try {
            network.timestamp = 0;//MeshParserUtils.parseTimeStamp(jsonObject.get("timestamp").getAsString());
        } catch (Exception ex) {
            throw new JsonSyntaxException("Invalid Mesh Provisioning/Configuration Database JSON file, " +
                    "mesh network timestamp must follow the Mesh Provisioning/Configuration Database format.");
        }
        network.netKeys = deserializeNetKeys(context,
                jsonObject.getAsJsonArray("netKeys"), network.meshUUID);
        network.appKeys = deserializeAppKeys(context,
                jsonObject.getAsJsonArray("appKeys"), network.meshUUID);
        network.provisioners = deserializeProvisioners(context,
                jsonObject.getAsJsonArray("provisioners"), network.meshUUID);

        if (jsonObject.has("nodes"))
            network.nodes = deserializeNodes(context,
                    jsonObject.getAsJsonArray("nodes"), network.meshUUID);

        if (jsonObject.has("groups"))
            network.groups = deserializeGroups(jsonObject, network.meshUUID);

        if (jsonObject.has("scenes"))
            network.scenes = deserializeScenes(jsonObject, network.meshUUID);

        assignProvisionerAddresses(network);

        return network;
    }

    @Override
    public JsonElement serialize(final MeshNetwork network,
                                 final Type typeOfSrc,
                                 final JsonSerializationContext context) {
        final String meshUuid = MeshParserUtils.uuidToHex(network.getMeshUUID());
        final JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("$schema", network.getSchema());
        jsonObject.addProperty("id", network.getId());
        jsonObject.addProperty("version", network.getVersion());
        jsonObject.addProperty("meshUUID", meshUuid);
        jsonObject.addProperty("meshName", network.getMeshName());
        jsonObject.addProperty("timestamp", MeshParserUtils.formatTimeStamp(network.getTimestamp()));
        jsonObject.add("netKeys", serializeNetKeys(context, network.getNetKeys()));
        jsonObject.add("appKeys", serializeAppKeys(context, network.getAppKeys()));
        jsonObject.add("provisioners", serializeProvisioners(context, network.getProvisioners()));

        //Optional properties
        if (!network.getNodes().isEmpty()) {
            jsonObject.add("nodes", serializeNodes(context, network.getNodes()));
        }

        //Optional properties
        if (!network.getGroups().isEmpty())
            jsonObject.add("groups", serializeGroups(network.getGroups()));

        //Mandatory properties
        if (!network.getScenes().isEmpty())
            jsonObject.add("scenes", serializeScenes(network.getScenes()));

        return jsonObject;
    }

    /**
     * Validates the mesh object by checking if the document contains the mandatory fields
     *
     * @param mesh json
     * @return true if valid and false otherwise
     */
    private boolean isValidMeshObject(@NonNull final JsonObject mesh) {
        return mesh.has("meshUUID") &&
                mesh.has("meshName") &&
                mesh.has("timestamp") &&
                mesh.has("provisioners") &&
                mesh.has("netKeys") &&
                mesh.has("appKeys") &&
                mesh.has("nodes");
    }

    /**
     * Returns a JsonElement of application keys keys after serializing the network keys
     *
     * @param context     Serializer context
     * @param networkKeys Network key list
     * @return JsonElement
     */
    private JsonElement serializeNetKeys(@NonNull final JsonSerializationContext context,
                                         @NonNull final List<NetworkKey> networkKeys) {
        final Type networkKey = new TypeToken<List<NetworkKey>>() {
        }.getType();
        return context.serialize(networkKeys, networkKey);
    }

    /**
     * Returns a JsonElement of network keys after de-serializing the json array containing the network keys
     *
     * @param context  deserializer context
     * @param json     json array containing the netkeys
     * @param meshUuid network provisionerUuid
     * @return List of network keys
     */
    private List<NetworkKey> deserializeNetKeys(@NonNull final JsonDeserializationContext context,
                                                @NonNull final JsonArray json,
                                                @NonNull final String meshUuid) {
        final Type networkKey = new TypeToken<List<NetworkKey>>() {
        }.getType();
        final List<NetworkKey> networkKeys = context.deserialize(json, networkKey);
        for (NetworkKey key : networkKeys) {
            key.setMeshUuid(meshUuid);
        }
        return networkKeys;
    }

    /**
     * Returns a list of network keys after serializing application keys list
     *
     * @param context         Serializer context
     * @param applicationKeys Application key list
     * @return JsonElement
     */
    private JsonElement serializeAppKeys(@NonNull final JsonSerializationContext context,
                                         @NonNull final List<ApplicationKey> applicationKeys) {
        final Type networkKey = new TypeToken<List<ApplicationKey>>() {
        }.getType();
        return context.serialize(applicationKeys, networkKey);
    }

    /**
     * Returns a list of application keys after de-serializing the json array containing the application keys
     *
     * @param context  deserializer context
     * @param json     json array containing the app keys
     * @param meshUuid network provisionerUuid
     * @return List of app keys
     */
    private List<ApplicationKey> deserializeAppKeys(@NonNull final JsonDeserializationContext context,
                                                    @NonNull final JsonArray json,
                                                    @NonNull final String meshUuid) {
        final Type applicationKeyList = new TypeToken<List<ApplicationKey>>() {
        }.getType();
        final List<ApplicationKey> applicationKeys = context.deserialize(json, applicationKeyList);
        for (ApplicationKey key : applicationKeys) {
            key.setMeshUuid(meshUuid);
        }
        return applicationKeys;
    }

    /**
     * Returns a list of nodes de-serializing the json array containing the provisioners
     *
     * @param context  deserializer context
     * @param json     json array object containing the provisioners
     * @param meshUuid network provisionerUuid
     * @return List of nodes
     */
    private List<Provisioner> deserializeProvisioners(@NonNull final JsonDeserializationContext context,
                                                      @NonNull final JsonArray json,
                                                      @NonNull final String meshUuid) {
        List<Provisioner> provisioners = new ArrayList<>();
        final JsonArray jsonProvisioners = json.getAsJsonArray();
        for (int i = 0; i < jsonProvisioners.size(); i++) {
            final JsonObject jsonProvisioner = jsonProvisioners.get(i).getAsJsonObject();
            final String name = jsonProvisioner.get("provisionerName").getAsString();
            final String uuid = jsonProvisioner.get("UUID").getAsString().toUpperCase();
            final String provisionerUuid = MeshParserUtils.formatUuid(uuid);

            if (provisionerUuid == null)
                throw new IllegalArgumentException("Invalid Mesh Provisioning/Configuration " +
                        "Database, invalid provisioner uuid.");

            final List<AllocatedUnicastRange> unicastRanges = deserializeAllocatedUnicastRange(context,
                    jsonProvisioner);
            List<AllocatedGroupRange> groupRanges = new ArrayList<>();
            if (jsonProvisioner.has("allocatedGroupRange")) {
                if (!jsonProvisioner.get("allocatedGroupRange").isJsonNull()) {
                    groupRanges = deserializeAllocatedGroupRange(context, jsonProvisioner);
                }
            }

            List<AllocatedSceneRange> sceneRanges = new ArrayList<>();
            if (jsonProvisioner.has("allocatedSceneRange")) {
                if (!jsonProvisioner.get("allocatedSceneRange").isJsonNull()) {
                    sceneRanges = deserializeAllocatedSceneRange(context, jsonProvisioner);
                }
            }

            final Provisioner provisioner = new Provisioner(provisionerUuid,
                    unicastRanges, groupRanges, sceneRanges, meshUuid);
            provisioner.setProvisionerName(name);
            provisioners.add(provisioner);
        }
        return provisioners;
    }

    /**
     * Returns serialized json element containing the provisioners
     *
     * @param context      Serializer context
     * @param provisioners Provisioners list
     * @return JsonElement
     */
    private JsonElement serializeProvisioners(@NonNull final JsonSerializationContext context,
                                              @NonNull final List<Provisioner> provisioners) {
        final JsonArray jsonArray = new JsonArray();
        for (Provisioner provisioner : provisioners) {
            final JsonObject provisionerJson = new JsonObject();
            provisionerJson.addProperty("provisionerName", provisioner.getProvisionerName());
            provisionerJson.addProperty("UUID", MeshParserUtils.uuidToHex(provisioner.getProvisionerUuid()));
            provisionerJson.add("allocatedUnicastRange",
                    serializeAllocatedUnicastRanges(context, provisioner.allocatedUnicastRanges));

            provisionerJson.add("allocatedGroupRange",
                    serializeAllocatedGroupRanges(context, provisioner.allocatedGroupRanges));

            provisionerJson.add("allocatedSceneRange",
                    serializeAllocatedSceneRanges(context, provisioner.allocatedSceneRanges));
            jsonArray.add(provisionerJson);
        }
        return jsonArray;
    }

    /**
     * Returns serialized json element containing the allocated unicast ranges
     *
     * @param context Serializer context
     * @param ranges  allocated group range
     */
    private JsonElement serializeAllocatedUnicastRanges(@NonNull final JsonSerializationContext context,
                                                        @NonNull final List<AllocatedUnicastRange> ranges) {
        final Type allocatedUnicastRanges = new TypeToken<List<AllocatedUnicastRange>>() {
        }.getType();
        return context.serialize(ranges, allocatedUnicastRanges);
    }

    /**
     * Returns a list of allocated unicast ranges allocated to a provisioner
     *
     * @param context deserializer context
     * @param json    json network object containing the provisioners
     */
    private List<AllocatedUnicastRange> deserializeAllocatedUnicastRange(@NonNull final JsonDeserializationContext context,
                                                                         @NonNull final JsonObject json) {
        final Type unicastRangeList = new TypeToken<List<AllocatedUnicastRange>>() {
        }.getType();
        return context.deserialize(json.get("allocatedUnicastRange").getAsJsonArray(), unicastRangeList);
    }

    /**
     * Returns serialized json element containing the allocated group ranges
     *
     * @param context Serializer context
     * @param ranges  allocated group range
     */
    private JsonElement serializeAllocatedGroupRanges(@NonNull final JsonSerializationContext context,
                                                      @NonNull final List<AllocatedGroupRange> ranges) {
        final Type allocatedGroupRanges = new TypeToken<List<AllocatedGroupRange>>() {
        }.getType();
        return context.serialize(ranges, allocatedGroupRanges);
    }

    /**
     * Returns a list of nodes de-serializing the json array containing the allocated unicast range list
     *
     * @param context deserializer context
     * @param json    json network object containing the provisioners
     */
    private List<AllocatedGroupRange> deserializeAllocatedGroupRange(@NonNull final JsonDeserializationContext context,
                                                                     @NonNull final JsonObject json) {
        final Type groupRangeList = new TypeToken<List<AllocatedGroupRange>>() {
        }.getType();
        return context.deserialize(json.getAsJsonArray("allocatedGroupRange"), groupRangeList);
    }

    /**
     * Returns serialized json element containing the allocated scene ranges
     *
     * @param context Serializer context
     * @param ranges  Allocated scene range
     */
    private JsonElement serializeAllocatedSceneRanges(@NonNull final JsonSerializationContext context,
                                                      @NonNull final List<AllocatedSceneRange> ranges) {
        final Type allocatedSceneRanges = new TypeToken<List<AllocatedSceneRange>>() {
        }.getType();
        return context.serialize(ranges, allocatedSceneRanges);
    }

    /**
     * Returns a list of nodes de-serializing the json array containing the allocated unicast range list
     *
     * @param context deserializer context
     * @param json    json network object containing the provisioners
     */
    private List<AllocatedSceneRange> deserializeAllocatedSceneRange(@NonNull final JsonDeserializationContext context,
                                                                     @NonNull final JsonObject json) {
        final Type sceneRangeList = new TypeToken<List<AllocatedSceneRange>>() {
        }.getType();
        return context.deserialize(json.getAsJsonArray("allocatedSceneRange"), sceneRangeList);
    }

    /**
     * Returns serialized json element containing the nodes
     *
     * @param context Serializer context
     * @param nodes   Nodes list
     * @return JsonElement
     */
    private JsonElement serializeNodes(@NonNull final JsonSerializationContext context,
                                       @NonNull final List<ProvisionedMeshNode> nodes) {
        final Type nodeList = new TypeToken<List<ProvisionedMeshNode>>() {
        }.getType();
        return context.serialize(nodes, nodeList);
    }

    /**
     * Returns a list of nodes deserializing the json array containing the provisioned mesh nodes
     *
     * @param context  deserializer context
     * @param json     json array containing the nodes
     * @param meshUuid network provisionerUuid
     * @return List of nodes
     */
    private List<ProvisionedMeshNode> deserializeNodes(@NonNull final JsonDeserializationContext context,
                                                       @NonNull final JsonArray json, final String meshUuid) {
        final Type nodeList = new TypeToken<List<ProvisionedMeshNode>>() {
        }.getType();
        final List<ProvisionedMeshNode> nodes = context.deserialize(json, nodeList);
        for (ProvisionedMeshNode node : nodes) {
            node.setMeshUuid(meshUuid);
        }
        return nodes;
    }

    /**
     * Returns serialized json element containing the groups
     *
     * @param groups Group list
     * @return JsonElement
     */
    private JsonElement serializeGroups(@NonNull final List<Group> groups) {
        JsonArray groupsArray = new JsonArray();
        for (Group group : groups) {
            JsonObject groupObj = new JsonObject();
            groupObj.addProperty("name", group.getName());
            if (group.getAddressLabel() == null) {
                groupObj.addProperty("address", MeshAddress.formatAddress(group.getAddress(), false));
            } else {
                groupObj.addProperty("address", MeshParserUtils.uuidToHex(group.getAddressLabel()));
            }
            groupObj.addProperty("parentAddress", MeshAddress.formatAddress(group.getParentAddress(), false));
            groupsArray.add(groupObj);
        }
        return groupsArray;
    }

    /**
     * Returns a list of groups de-serializing the json array containing the groups
     *
     * @param jsonNetwork json network object containing the groups
     * @param meshUuid    network provisionerUuid
     * @return List of nodes
     */
    private List<Group> deserializeGroups(@NonNull final JsonObject jsonNetwork,
                                          @NonNull final String meshUuid) {
        final List<Group> groups = new ArrayList<>();
        if (!jsonNetwork.has("groups"))
            return groups;

        final JsonArray jsonGroups = jsonNetwork.getAsJsonArray("groups");
        for (int i = 0; i < jsonGroups.size(); i++) {
            try {
                final JsonObject jsonGroup = jsonGroups.get(i).getAsJsonObject();
                final String name = jsonGroup.get("name").getAsString();
                final String address = MeshParserUtils.formatUuid(jsonGroup.get("address").getAsString());
                final String parentAddress = MeshParserUtils.formatUuid(jsonGroup.get("parentAddress").getAsString());
                final Group group;
                if (address == null) {
                    if (parentAddress == null) {
                        group = new Group(Integer.parseInt(jsonGroup.get("address").getAsString(), 16),
                                Integer.parseInt(jsonGroup.get("parentAddress").getAsString(), 16), meshUuid);
                    } else {
                        group = new Group(Integer.parseInt(jsonGroup.get("address").getAsString(), 16),
                                UUID.fromString(parentAddress), meshUuid);
                    }
                } else {
                    if (parentAddress == null) {
                        group = new Group(UUID.fromString(address),
                                Integer.parseInt(jsonGroup.get("parentAddress").getAsString(), 16), meshUuid);
                    } else {
                        group = new Group(UUID.fromString(address),
                                UUID.fromString(parentAddress), meshUuid);
                    }
                }
                group.setName(name);
                groups.add(group);
            } catch (Exception ex) {
                Log.e(TAG, "Error while de-serializing groups: " + ex.getMessage());
            }
        }
        return groups;
    }

    /**
     * Returns serialized json element containing the scenes
     *
     * @param scenes Group list
     * @return JsonElement
     */
    private JsonElement serializeScenes(@NonNull final List<Scene> scenes) {
        JsonArray scenesArray = new JsonArray();
        for (Scene scene : scenes) {
            JsonObject sceneObj = new JsonObject();
            sceneObj.addProperty("name", scene.getName());
            final JsonArray array = new JsonArray();
            for (Integer address : scene.getAddresses()) {
                array.add(MeshAddress.formatAddress(address, false));
            }
            sceneObj.add("addresses", array);
            sceneObj.addProperty("number", String.format(Locale.US, "%04X", scene.getNumber()));
            scenesArray.add(sceneObj);
        }
        return scenesArray;
    }

    /**
     * Returns a list of scenes de-serializing the json array containing the scenes
     *
     * @param jsonNetwork json array containing the scenes
     * @param meshUuid    network provisionerUuid
     * @return List of nodes
     */
    private List<Scene> deserializeScenes(@NonNull final JsonObject jsonNetwork,
                                          @NonNull final String meshUuid) {
        final List<Scene> scenes = new ArrayList<>();
        try {
            if (!jsonNetwork.has("scenes"))
                return scenes;

            final JsonArray jsonScenes = jsonNetwork.getAsJsonArray("scenes");
            for (int i = 0; i < jsonScenes.size(); i++) {
                final JsonObject jsonScene = jsonScenes.get(i).getAsJsonObject();
                final String name = jsonScene.get("name").getAsString();
                final List<Integer> addresses = new ArrayList<>();
                if (jsonScene.has("addresses")) {
                    final JsonArray addressesArray = jsonScene.get("addresses").getAsJsonArray();
                    for (int j = 0; j < addressesArray.size(); j++) {
                        addresses.add(Integer.parseInt(addressesArray.get(j).getAsString(), 16));
                    }
                }
                final int number = jsonScene.get("number").getAsInt();
                final Scene scene = new Scene(number, addresses, meshUuid);
                scene.setName(name);
            }
        } catch (Exception ex) {
            Log.e(TAG, "Error while de-serializing scenes: " + ex.getMessage());
        }
        return scenes;
    }

    /**
     * Returns the next available address based on the nodes/elements in the network
     *
     * @param nodes provisioned nodes
     */
    private int getNextAvailableAddress(final List<ProvisionedMeshNode> nodes) {
        //We set the next available unicast address here, this is a library attribute
        int unicast = 1;
        if (nodes != null && !nodes.isEmpty()) {
            final int index = nodes.size() - 1;
            final ProvisionedMeshNode node = nodes.get(index);
            Map<Integer, Element> elements = node.getElements();
            if (elements != null && !elements.isEmpty()) {
                unicast = node.getUnicastAddress() + elements.size();
            } else {
                unicast = node.getUnicastAddress() + 1;
            }
        }
        return unicast;
    }

    private void assignProvisionerAddresses(@NonNull final MeshNetwork network) {
        for (Provisioner provisioner : network.provisioners) {
            for (ProvisionedMeshNode node : network.nodes) {
                if (provisioner.getProvisionerUuid().equalsIgnoreCase(node.getUuid())) {
                    provisioner.assignProvisionerAddress(node.getUnicastAddress());
                    provisioner.setGlobalTtl(node.getTtl());
                }
            }
        }
    }
}
