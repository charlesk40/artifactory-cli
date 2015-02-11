package org.artifactory.cli.command;

import org.artifactory.cli.common.Command;
import org.artifactory.cli.common.UrlBasedCommand;
import org.artifactory.cli.main.CliLog;
import org.artifactory.cli.main.CliOption;
import org.artifactory.cli.main.CommandDefinition;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

/**
 * 
 * @author charlesk
 * 
 */
public class MoveCommand extends UrlBasedCommand implements Command {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public MoveCommand() {
        super(CommandDefinition.move, CliOption.dryrun, CliOption.moveFrom, CliOption.moveTo);
    }

    public int execute() throws Exception {
        boolean isDryRunSet = CliOption.dryrun.isSet();
        boolean isMoveFromSet = CliOption.moveFrom.isSet();
        boolean isMoveToSet = CliOption.moveTo.isSet();
        if (!isMoveFromSet) {
            System.err.println("Cannot execute move command without from path.  Please set --moveFrom");
            return 3;
        }
        if (!isMoveToSet) {
            System.err.println("Cannot execute move command without to path.  Please set --moveTo");
            return 3;
        }
        CliLog.info("moving from " + CliOption.moveFrom.getValue() + " to " + CliOption.moveTo.getValue());
        if (isMoveFromSet && isMoveToSet) {
            // get list of folders
            StringBuilder urlBuilder = new StringBuilder();
            String moveFrom = CliOption.moveFrom.getValue();
            String moveTo = CliOption.moveTo.getValue();
            String baseUrl = getUrl();
            urlBuilder.append(baseUrl).append("storage/").append(moveFrom);
            CliLog.info("Calling REST api " + urlBuilder.toString() + " to get list of sub folders");
            byte[] content =
                            get(urlBuilder.toString(), 200,
                                            "application/vnd.org.jfrog.artifactory.storage.FolderInfo+json", false);
            JsonNode rootNode = objectMapper.readTree(content);
            Iterator<JsonNode> childrenNodes = rootNode.get("children").getElements();
            CliLog.info(moveFrom + " contains " + rootNode.get("children").size() + " childrens");
            long progress = 1;
            while (childrenNodes.hasNext()) {
                JsonNode childrenNode = childrenNodes.next();
                String subFolderName = childrenNode.get("uri").getTextValue();
                boolean isFolder = childrenNode.get("folder").getBooleanValue();
                CliLog.info("folder name " + subFolderName + " folder " + isFolder);
                // now, move one by one by calling move item Rest API.
                // http://www.jfrog.com/confluence/display/RTF/Artifactory+REST+API#ArtifactoryRESTAPI-MoveItem
                StringBuilder moveUrlBuilder = new StringBuilder(baseUrl);
                moveUrlBuilder.append("move/").append(moveFrom).append(subFolderName).append("?to=/").append(moveTo)
                                .append("&dry=").append((isDryRunSet ? 1 : 0));
                CliLog.info(progress + "/" + rootNode.get("children").size() + " Calling move REST API "
                                + moveUrlBuilder.toString());
//                post(moveUrlBuilder.toString(), null, null, 200,
//                                "application/vnd.org.jfrog.artifactory.storage.CopyOrMoveResult+json", true);
//                StringBuilder verifyUrlBuilder = new StringBuilder(baseUrl);
//                verifyUrlBuilder.append("storage/").append(moveTo).append("/").append(subFolderName);
//                CliLog.info("Verifing the move: " + verifyUrlBuilder.toString());
//                get(verifyUrlBuilder.toString(), 200, null, false);
                progress++;
                TimeUnit.SECONDS.sleep(5);
                break;
            }
        }
        return 0;
    }

    public void usage() {
        defaultUsage();
    }

}
