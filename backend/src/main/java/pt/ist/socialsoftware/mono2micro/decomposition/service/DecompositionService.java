package pt.ist.socialsoftware.mono2micro.decomposition.service;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pt.ist.socialsoftware.mono2micro.cluster.Cluster;
import pt.ist.socialsoftware.mono2micro.clusteringAlgorithm.Clustering;
import pt.ist.socialsoftware.mono2micro.clusteringAlgorithm.Expert;
import pt.ist.socialsoftware.mono2micro.decomposition.domain.Decomposition;
import pt.ist.socialsoftware.mono2micro.decomposition.dto.request.DecompositionRequest;
import pt.ist.socialsoftware.mono2micro.decomposition.repository.DecompositionRepository;
import pt.ist.socialsoftware.mono2micro.element.Element;
import pt.ist.socialsoftware.mono2micro.history.service.HistoryService;
import pt.ist.socialsoftware.mono2micro.operation.formCluster.FormClusterOperation;
import pt.ist.socialsoftware.mono2micro.operation.merge.MergeOperation;
import pt.ist.socialsoftware.mono2micro.operation.rename.RenameOperation;
import pt.ist.socialsoftware.mono2micro.operation.split.SplitOperation;
import pt.ist.socialsoftware.mono2micro.operation.transfer.TransferOperation;
import pt.ist.socialsoftware.mono2micro.similarity.domain.Similarity;
import pt.ist.socialsoftware.mono2micro.similarity.repository.SimilarityRepository;
import pt.ist.socialsoftware.mono2micro.strategy.domain.Strategy;
import pt.ist.socialsoftware.mono2micro.strategy.repository.StrategyRepository;

import java.util.Optional;

@Service
public class DecompositionService {
    @Autowired
    StrategyRepository strategyRepository;

    @Autowired
    SimilarityRepository similarityRepository;

    @Autowired
    DecompositionRepository decompositionRepository;

    @Autowired
    HistoryService historyService;

    public void createDecomposition(DecompositionRequest request) throws Exception {
        Similarity similarity = similarityRepository.findByName(request.getSimilarityName());
        
        Clustering clustering = similarity.getClustering();
        Decomposition decomposition = clustering.generateDecomposition(similarity, request);

        setupDecomposition(decomposition);
    }

    public void createExpertDecomposition(String similarityName, String expertName, Optional<MultipartFile> expertFile) throws Exception {
        Similarity similarity = similarityRepository.findByName(similarityName);
        Expert expert = new Expert();

        Decomposition decomposition = expert.generateClusters(similarity, expertName, expertFile);

        setupDecomposition(decomposition);
    }

    public void setupDecomposition(Decomposition decomposition) throws Exception {
        decomposition.setup();
        decomposition.calculateMetrics();

        historyService.saveHistory(decomposition.getHistory());
        decompositionRepository.save(decomposition);
        similarityRepository.save(decomposition.getSimilarity());
        strategyRepository.save(decomposition.getStrategy());
    }

    public Decomposition updateDecomposition(String decompositionName) throws Exception {
        Decomposition decomposition = decompositionRepository.findByName(decompositionName);
        if (decomposition.isOutdated()) {
            decomposition.update();
            decomposition.calculateMetrics();
            decomposition.setOutdated(false);
            decompositionRepository.save(decomposition);
        }
        return decomposition;
    }

    public Decomposition getDecomposition(String decompositionName) {
        return decompositionRepository.findByName(decompositionName);
    }

    public void deleteSingleDecomposition(String decompositionName) {
        Decomposition decomposition = decompositionRepository.findByName(decompositionName);
        decomposition.deleteProperties();

        Strategy strategy = decomposition.getStrategy();
        strategy.removeDecomposition(decomposition.getName());
        Similarity similarity = decomposition.getSimilarity();
        similarity.removeDecomposition(decomposition.getName());

        similarityRepository.save(similarity);
        historyService.deleteHistory(decomposition.getHistory());
        strategyRepository.save(strategy);
        decompositionRepository.deleteByName(decomposition.getName());
    }

    public void deleteDecomposition(Decomposition decomposition) {
        decomposition.deleteProperties();
        historyService.deleteHistory(decomposition.getHistory());
        decompositionRepository.deleteByName(decomposition.getName());
    }

    public JSONObject exportDecomposition(String decompositionName) throws JSONException {
        Decomposition decomposition = decompositionRepository.findByName(decompositionName);
        return buildJsonData(decomposition);
    }

    private JSONObject buildJsonData(Decomposition decomposition) throws JSONException {

        JSONObject data = new JSONObject();
        JSONObject decompositionObject = new JSONObject();
        JSONArray clustersList = new JSONArray();

        for (Cluster cluster : decomposition.getClusters().values()) {
            JSONObject clusterObject = new JSONObject();
            clusterObject.put("name", cluster.getName());

            JSONArray elementsList = new JSONArray();
            for (Element element : cluster.getElements()) {
                JSONObject elementObject = new JSONObject();
                elementObject.put("name", element.getName());
                elementsList.put(elementObject);
            }

            clusterObject.put("elements", elementsList);
            clustersList.put(clusterObject);
        }

        decompositionObject.put("clusters", clustersList);
        data.put("decomposition", decompositionObject);
        return data;
    }

    public void mergeClustersOperation(String decompositionName, MergeOperation operation) {
        Decomposition decomposition = decompositionRepository.findByName(decompositionName);
        decomposition.mergeClusters(operation);
        historyService.saveHistory(decomposition.getHistory());
        decompositionRepository.save(decomposition);
    }

    public void renameClusterOperation(String decompositionName, RenameOperation operation) {
        Decomposition decomposition = decompositionRepository.findByName(decompositionName);
        decomposition.renameCluster(operation);
        historyService.saveHistory(decomposition.getHistory());
        decompositionRepository.save(decomposition);
    }

    public void splitClusterOperation(String decompositionName, SplitOperation operation) {
        Decomposition decomposition = decompositionRepository.findByName(decompositionName);
        decomposition.splitCluster(operation);
        historyService.saveHistory(decomposition.getHistory());
        decompositionRepository.save(decomposition);
    }

    public void transferEntitiesOperation(String decompositionName, TransferOperation operation) {
        Decomposition decomposition = decompositionRepository.findByName(decompositionName);
        decomposition.transferEntities(operation);
        historyService.saveHistory(decomposition.getHistory());
        decompositionRepository.save(decomposition);
    }

    public void formClusterOperation(String decompositionName, FormClusterOperation operation) {
        Decomposition decomposition = decompositionRepository.findByName(decompositionName);
        decomposition.formCluster(operation);
        historyService.saveHistory(decomposition.getHistory());
        decompositionRepository.save(decomposition);
    }

    public String getEdgeWeights(String decompositionName, String representationInfo) throws Exception {
        Decomposition clusterView = decompositionRepository.findByName(decompositionName);
        return clusterView.getEdgeWeights(representationInfo);
    }

    public String getSearchItems(String decompositionName, String representationInfo) throws Exception {
        Decomposition clusterView = decompositionRepository.findByName(decompositionName);
        return clusterView.getSearchItems(representationInfo);
    }

    public void snapshotDecomposition(String decompositionName) throws Exception {
        Decomposition decomposition = updateDecomposition(decompositionName);
        Similarity similarity = decomposition.getSimilarity();

        String snapshotName = decomposition.getName() + " SNAPSHOT";
        if (similarity.getDecompositionByName(snapshotName) != null) {
            int i = 1;
            do {i++;} while (similarity.getDecompositionByName(snapshotName + "(" + i + ")") != null);
            snapshotName = snapshotName + "(" + i + ")";
        }

        Decomposition snapshotDecomposition = decomposition.snapshotDecomposition(snapshotName);

        decompositionRepository.save(snapshotDecomposition);
        similarityRepository.save(similarity);
        strategyRepository.save(similarity.getStrategy());
    }
}