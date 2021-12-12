/*
 * The MIT License
 *
 * Copyright 2021 Jawamaster (Arthur Bulin).
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package net.jawasystems.jawacore.handlers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import net.jawasystems.jawacore.JawaCore;
import org.elasticsearch.client.indexlifecycle.FreezeAction;
import org.elasticsearch.client.indexlifecycle.GetLifecyclePolicyRequest;
import org.elasticsearch.client.indexlifecycle.LifecycleAction;
import org.elasticsearch.client.indexlifecycle.LifecyclePolicy;
import org.elasticsearch.client.indexlifecycle.Phase;
import org.elasticsearch.client.indexlifecycle.PutLifecyclePolicyRequest;
import org.elasticsearch.client.indexlifecycle.ReadOnlyAction;
import org.elasticsearch.client.indexlifecycle.RolloverAction;
import org.elasticsearch.client.indexlifecycle.SetPriorityAction;
import org.elasticsearch.client.indexlifecycle.ShrinkAction;
import org.elasticsearch.client.indices.CreateDataStreamRequest;
import org.elasticsearch.client.indices.GetComponentTemplatesRequest;
import org.elasticsearch.client.indices.GetComposableIndexTemplateRequest;
import org.elasticsearch.client.indices.PutComponentTemplateRequest;
import org.elasticsearch.client.indices.PutComposableIndexTemplateRequest;
import org.elasticsearch.cluster.metadata.ComponentTemplate;
import org.elasticsearch.cluster.metadata.ComposableIndexTemplate;
import org.elasticsearch.cluster.metadata.Template;
import org.elasticsearch.common.compress.CompressedXContent;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.core.TimeValue;

/**
 *
 * @author Jawamaster (Arthur Bulin)
 */
public class IndexHandler {
    private static final Logger LOGGER = Logger.getLogger("IndexHandler");
    private static final String COMPONENTNAME = "chatlog-minecraft";
    
    /** Validate or build all components needed to put the chatlog datastream together.
     * @param chatIndexIdentity This is the arbitrary index identifier
     * @return True if the DataStream is created, false if not or on error
     */
    public static boolean createChatLog(String chatIndexIdentity) {
        LOGGER.log(Level.INFO, "Creating a Data Stream of {0}-{1}", new Object[]{COMPONENTNAME, chatIndexIdentity});
        
        //Resolve datastream dependent templates and policies
        if (validateLifeCylePolicy() && validateComponentTemplate() && validateIndexTemplate()) {
            LOGGER.log(Level.INFO, "All DataStream dependent components validated/created");
        } else {
            return false;
        }

        //Will never need to validate DataStream here, we only enter this method if it doesn't exist
        if (!createDataStream(chatIndexIdentity)) {
            LOGGER.log(Level.WARNING, "Data Stream creation failed!");
            return false;
        } else {
            LOGGER.log(Level.INFO, "Data Stream created");
            return true;
        }
    }
    
    /** Validates the index template and attempts to create it if it does not exist.
     * CAUTION: This runs in the main thread
     * @return True if the index template exists, or is created. False if the index template creation fails or version validation fails
     */
    private static boolean validateIndexTemplate(){
        GetComposableIndexTemplateRequest request = new GetComposableIndexTemplateRequest(COMPONENTNAME);
        if (!ESHandler.checkIndexTemplate(request, COMPONENTNAME)) {
            if (!createIndexTemplate()) {
                LOGGER.log(Level.WARNING, "Index Template creation failed!");
                return false;
            } else {
                LOGGER.log(Level.INFO, "Index Template created");
                return true;
            }
        } else {
            LOGGER.log(Level.INFO, "Index Template exists and validates");
            return true;
        }
    }
    
    /** Validates the component template and attempts to create it if it does not exist.
     * CAUTION: This runs in the main thread
     * @return True if the component template exists, or is created. False if the component template creation fails or version validation fails
     */
    private static boolean validateComponentTemplate(){
        GetComponentTemplatesRequest request = new GetComponentTemplatesRequest(COMPONENTNAME);
        if (!ESHandler.checkComponentTemplate(request, COMPONENTNAME)) {
            if (!createComponentTemplate()) {
                LOGGER.log(Level.WARNING, "Component Template creation failed!");
                return false;
            } else {
                LOGGER.log(Level.INFO, "Component Template created");
                return true;
            }
        } else {
            LOGGER.log(Level.INFO, "Component Template exists and validates");
            return true;
        }
    }
    
    /** Validates the lifecyle policy and attempts to create it if it does not exist.
     * CAUTION: This runs in the main thread WARNING: RETURNS TRUE DO TO A BUG IN ES!!!
     * @return True if the index exists, or is created. False if the policy creation fails or version validation fails
     */
    private static boolean validateLifeCylePolicy(){
        //There's a bug with the ES HighLevel Client and it doesn't load the right xcontent parsing. I think this has something to do about the class loader. It works in ESInit but not here.
        return true;
//        GetLifecyclePolicyRequest lcpRequest = new GetLifecyclePolicyRequest("RollingChatLogPolicy");
//        if (!ESHandler.checkIndexLifeCyclePolicy(lcpRequest, "RollingChatLogPolicy")) {
//            if (!createLifeCyclePolicy()) {
//                LOGGER.log(Level.WARNING, "LifeCyle Policy creation failed!");
//                return false;
//            } else {
//                LOGGER.log(Level.INFO, "LifeCycle Policy created");
//                return true;
//            }
//        } else {
//            LOGGER.log(Level.INFO, "LifeCycle Policy exists and validates");
//            return true;
//        }
    }
    
    /** Create the necessary ComponentTemplates within ES that contains the settings and mappings
     * for the chatlog-minecraft index. It will then attempt to validate the component was completed successfully.
     * CAUTION: This runs in the main thread.
     * @return True if the component is created, false if not or on an error.
     */
    public static boolean createComponentTemplate() {
        InputStream io = JawaCore.class.getResourceAsStream("/chatlog-mappings.json");
        String br = new BufferedReader(new InputStreamReader(io)).lines().collect(Collectors.joining("\n"));

        Settings settings = Settings.builder()
                .put("index.number_of_shards", 4)
                .put("index.lifecycle.name", "RollingChatLogPolicy")
                .put("index.codec", "best_compression")
                .build();

        CompressedXContent mappings;
        try {
            mappings = new CompressedXContent(br);
            Template template = new Template(settings, mappings, null);

            Map<String, Object> metadata = new HashMap();
            metadata.put("datastream", null);
            ComponentTemplate comTemplate = new ComponentTemplate(template, 1L, null);

            PutComponentTemplateRequest request = new PutComponentTemplateRequest();
            request.name(COMPONENTNAME);
            request.componentTemplate(comTemplate);
            
            boolean isAcknowledged = ESHandler.createComponentTemplate(request);
            if (isAcknowledged) {
                GetComponentTemplatesRequest chRequest= new GetComponentTemplatesRequest(COMPONENTNAME);
                return ESHandler.checkComponentTemplate(chRequest, COMPONENTNAME);
            } else {
                LOGGER.log(Level.WARNING, "The ES server did not acknowledge the component template creation request");
                return false;
            }

        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            return false;
        }

    }
    
    /** Create the ComposableindexTemplate needed for the chatlog DataStream. It will attempt to 
     * validate the template once requested. CAUTION: This runs in the main thread.
     * @return True if the index template is created. False if failed or errored.
     */
    public static boolean createIndexTemplate() {
        PutComposableIndexTemplateRequest request = new PutComposableIndexTemplateRequest();
        request.name(COMPONENTNAME);
        
        List<String> patterns = Arrays.asList(COMPONENTNAME+"-*");
        List<String> componentTemplates = Arrays.asList(COMPONENTNAME);
        ComposableIndexTemplate composableTemplate = new ComposableIndexTemplate(patterns, null, componentTemplates, 500L, 1L, null, new ComposableIndexTemplate.DataStreamTemplate());
        
        request.indexTemplate(composableTemplate);
        
        boolean isAcknowledged = ESHandler.createIndexTemplate(request);
        if (isAcknowledged) {
            GetComposableIndexTemplateRequest chRequest = new GetComposableIndexTemplateRequest(COMPONENTNAME);
            return ESHandler.checkIndexTemplate(chRequest, COMPONENTNAME);
        } else {
            LOGGER.log(Level.WARNING, "The ES server did not acknowledge the index template creation request");
            return false;
        }
    }
    
    /** Create the life cycle policy needed for the chatlog DataStream. It will attempt to validate
     * the creation. CAUTION: This runs in the main thread.
     * @return True if the policy is created, false if failed or errored.
     */
    public static boolean createLifeCyclePolicy() {
        Map<String, Phase> phases = new HashMap<>();
        Map<String, LifecycleAction> hotActions = new HashMap();
        Map<String, LifecycleAction> warmActions = new HashMap();
        Map<String, LifecycleAction> coldActions = new HashMap();

        hotActions.put(RolloverAction.NAME, new RolloverAction(null, null, new TimeValue(182, TimeUnit.DAYS), null));
        hotActions.put(SetPriorityAction.NAME, new SetPriorityAction(100));
        phases.put("hot", new Phase("hot", TimeValue.ZERO, hotActions));

        warmActions.put(ShrinkAction.NAME, new ShrinkAction(1, null));
        warmActions.put(SetPriorityAction.NAME, new SetPriorityAction(50));
        warmActions.put(ReadOnlyAction.NAME, new ReadOnlyAction());
        phases.put("warm", new Phase("warm", TimeValue.timeValueDays(91), warmActions));
        
        coldActions.put(SetPriorityAction.NAME, new SetPriorityAction(0));
        coldActions.put(FreezeAction.NAME, new FreezeAction());
        phases.put("cold", new Phase("cold", TimeValue.timeValueDays(91), coldActions));
        
        LifecyclePolicy policy = new LifecyclePolicy("RollingChatLogPolicy", phases);
        PutLifecyclePolicyRequest request = new PutLifecyclePolicyRequest(policy);
        boolean isAcknowledged = ESHandler.createLifecyclePolicy(request);
        if (isAcknowledged) {
            GetLifecyclePolicyRequest chRequest = new GetLifecyclePolicyRequest("RollingChatLogPolicy");
            return ESHandler.checkIndexLifeCyclePolicy(chRequest, "RollingChatLogPolicy");
        } else {
            LOGGER.log(Level.WARNING, "The ES server did not acknowledge the life cycle policy creation request");
            return false;
        }
    }
    
    /** Create the Data Stream for the chatlog index. It will attempt to validate the creation.
     * CAUTION: This runs in the main thread.
     * @param chatIndexIdentity
     * @return True if created, false if failure or on error
     */
    public static boolean createDataStream(String chatIndexIdentity) {
        //CreateIndexRequest request = new CreateIndexRequest("logs-chat-minecraft");
        CreateDataStreamRequest request = new CreateDataStreamRequest("chatlog-minecraft-"+chatIndexIdentity);
        boolean isAcknowledged = ESHandler.createDataStream(request);
        if (isAcknowledged) {
            try {
                return ESHandler.indexExists("chatlog-minecraft-"+chatIndexIdentity);
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
                return false;
            }
        } else {
            LOGGER.log(Level.WARNING, "The ES server did not acknowledge the data stream creation request");
            return false;
        }
    }
}
