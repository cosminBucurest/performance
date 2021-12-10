package victor.training.performance.primitives.candy;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;


public class TableModel {

   private final ExecutorService candyClassificationUpdateExecuter;
   private final ICandyClassificationHandler candyClassificationHandler;

   private TableDataModel dataModel;

   private ICandyDataContainerListener currentContainerListener;

   public TableModel(
       ICandyClassificationHandler candyClassificationHandler,
       ExecutorService candyClassificationUpdateExecutor) {

      this.candyClassificationHandler = Objects.requireNonNull(candyClassificationHandler);
      this.candyClassificationUpdateExecuter = Objects.requireNonNull(candyClassificationUpdateExecutor);

      this.currentContainerListener = createContainerListener();
   }

   private ICandyDataContainerListener createContainerListener() {
      return new ICandyDataContainerListener() {
         @Override
         public void newData(Candy candy) {
            dataModel.addNewData(Collections.singletonList(candy));
            updateCandyRowModelWithClassifications(candy);
         }
         @Override
         public void removeAll() {
            // do similar things like add
         }
         @Override
         public void updateData(Candy updatedCandy, Candy oldCandy) {
         }
         @Override
         public void removeData(Candy sandy) {
         }

         @Override
         public void removeData(Collection<Candy> candys) {
         }

      };
   }
   public void setDataModel(TableDataModel dataModel) {
      this.dataModel = dataModel;
   }


   /**
    * Handles the change event of a container
    *
    * @param oldContainer the old container
    * @param newContainer the new container .. dah!
    */
   public void handleContainerSelectionChanged(
       ICandyDataContainer oldContainer,
       ICandyDataContainer newContainer) {

      // Unregister old container

      dataModel.removeAllData();
      registerContainer(newContainer);
   }

   // user controls the change of container.
   private void registerContainer(ICandyDataContainer container) { /// the container contains datasources
      container.activate();
      currentContainerListener = createContainerListener();
      container.addCandyDataContainerListener(currentContainerListener);
   }

   // 10k / minute  for each new candy
   private void updateCandyRowModelWithClassifications(Candy candy) {

      Consumer<List<String>> updateCandyWithClassifications = classifications -> {
         synchronized (TableModel.this) {

            dataModel.updatedData(candy, classifications);
         }
      };

      candyClassificationUpdateExecuter
          .execute(() -> candyClassificationHandler.handleIdentification(candy, updateCandyWithClassifications));
   }
}
