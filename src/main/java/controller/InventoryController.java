package controller;

import dao.MedicalInventoryDAO;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import model.MedicalInventory;

import java.sql.SQLException;

public class InventoryController {

    @FXML private TextField nameField;
    @FXML private TextField skuField;
    @FXML private TextField qtyField;
    @FXML private TextField reorderField;
    @FXML private TextField searchField;
    @FXML private Button    saveBtn;
    @FXML private Label     statusLabel;

    @FXML private TableView<MedicalInventory>              inventoryTable;
    @FXML private TableColumn<MedicalInventory, String>    colId;
    @FXML private TableColumn<MedicalInventory, String>    colName;
    @FXML private TableColumn<MedicalInventory, String>    colSku;
    @FXML private TableColumn<MedicalInventory, String>    colQty;
    @FXML private TableColumn<MedicalInventory, String>    colReorder;
    @FXML private TableColumn<MedicalInventory, String>    colStock;

    private final MedicalInventoryDAO inventoryDAO = new MedicalInventoryDAO();
    private long selectedId = -1;

    @FXML
    public void initialize() {
        setupColumns();
        loadAll();
        inventoryTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, row) -> { if (row != null) populateForm(row); }
        );
    }

    @FXML
    void onSave() {
        if (!validateInput()) return;
        try {
            MedicalInventory item = new MedicalInventory(
                nameField.getText().trim(),
                skuField.getText().trim(),
                Integer.parseInt(qtyField.getText().trim()),
                Integer.parseInt(reorderField.getText().trim())
            );
            if (selectedId == -1) {
                inventoryDAO.insert(item);
                setStatus("Item added to inventory.");
            } else {
                item.setId(selectedId);
                inventoryDAO.update(item);
                setStatus("Item updated.");
            }
            onClear();
            loadAll();
        } catch (NumberFormatException e) {
            setStatus("Quantity and Reorder Level must be numbers.");
        } catch (SQLException e) {
            setStatus("Error: " + e.getMessage());
        }
    }

    @FXML
    void onDelete() {
        MedicalInventory row = inventoryTable.getSelectionModel().getSelectedItem();
        if (row == null) { setStatus("Select an item to delete."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete \"" + row.getName() + "\" from inventory?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    inventoryDAO.delete(row.getId());
                    onClear();
                    loadAll();
                    setStatus("Item deleted.");
                } catch (SQLException e) { setStatus("Error: " + e.getMessage()); }
            }
        });
    }

    @FXML
    void onSearch() {
        String keyword = searchField.getText().trim();
        if (keyword.isBlank()) { loadAll(); return; }
        try {
            inventoryTable.setItems(FXCollections.observableArrayList(inventoryDAO.search(keyword)));
        } catch (SQLException e) { setStatus("Error: " + e.getMessage()); }
    }

    @FXML
    void onShowAll() { searchField.clear(); loadAll(); }

    @FXML
    void onLowStock() {
        try {
            inventoryTable.setItems(FXCollections.observableArrayList(inventoryDAO.findLowStock()));
            setStatus("Showing items at or below reorder level.");
        } catch (SQLException e) { setStatus("Error: " + e.getMessage()); }
    }

    @FXML
    void onClear() {
        nameField.clear(); skuField.clear(); qtyField.clear(); reorderField.clear();
        selectedId = -1;
        saveBtn.setText("Save Item");
        statusLabel.setText("");
        inventoryTable.getSelectionModel().clearSelection();
    }

    // -------------------------------------------------------------------------

    private boolean validateInput() {
        if (nameField.getText().isBlank())    { setStatus("Item name is required."); return false; }
        if (skuField.getText().isBlank())     { setStatus("SKU is required."); return false; }
        if (qtyField.getText().isBlank())     { setStatus("Quantity is required."); return false; }
        if (reorderField.getText().isBlank()) { setStatus("Reorder level is required."); return false; }
        try { Integer.parseInt(qtyField.getText().trim()); }
        catch (NumberFormatException e) { setStatus("Quantity must be a number."); return false; }
        try { Integer.parseInt(reorderField.getText().trim()); }
        catch (NumberFormatException e) { setStatus("Reorder level must be a number."); return false; }
        return true;
    }

    private void setupColumns() {
        colId.setCellValueFactory(c      -> new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        colName.setCellValueFactory(c    -> new SimpleStringProperty(c.getValue().getName()));
        colSku.setCellValueFactory(c     -> new SimpleStringProperty(c.getValue().getSku()));
        colQty.setCellValueFactory(c     -> new SimpleStringProperty(String.valueOf(c.getValue().getQuantityOnHand())));
        colReorder.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getReorderLevel())));
        colStock.setCellValueFactory(c   -> {
            MedicalInventory item = c.getValue();
            String label = item.getQuantityOnHand() <= item.getReorderLevel() ? "LOW STOCK" : "OK";
            return new SimpleStringProperty(label);
        });

        // Colour-code stock status
        colStock.setCellFactory(col -> new TableCell<>() {
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                setText(s);
                setStyle("LOW STOCK".equals(s)
                    ? "-fx-text-fill:#e53935; -fx-font-weight:bold;"
                    : "-fx-text-fill:#2e7d32; -fx-font-weight:bold;");
            }
        });
    }

    private void loadAll() {
        try {
            inventoryTable.setItems(FXCollections.observableArrayList(inventoryDAO.findAll()));
        } catch (SQLException e) { setStatus("Error loading inventory: " + e.getMessage()); }
    }

    private void populateForm(MedicalInventory item) {
        selectedId = item.getId();
        nameField.setText(item.getName());
        skuField.setText(item.getSku());
        qtyField.setText(String.valueOf(item.getQuantityOnHand()));
        reorderField.setText(String.valueOf(item.getReorderLevel()));
        saveBtn.setText("Update Item");
        setStatus("Editing: " + item.getName());
    }

    private void setStatus(String msg) { if (statusLabel != null) statusLabel.setText(msg); }
}
