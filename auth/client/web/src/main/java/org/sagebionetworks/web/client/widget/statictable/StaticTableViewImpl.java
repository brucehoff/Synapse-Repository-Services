package org.sagebionetworks.web.client.widget.statictable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.web.client.DisplayConstants;

import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.data.BaseModelData;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.Status;
import com.extjs.gxt.ui.client.widget.grid.ColumnConfig;
import com.extjs.gxt.ui.client.widget.grid.ColumnModel;
import com.extjs.gxt.ui.client.widget.grid.Grid;
import com.extjs.gxt.ui.client.widget.layout.FitLayout;
import com.extjs.gxt.ui.client.widget.layout.FlowLayout;
import com.extjs.gxt.ui.client.widget.tips.QuickTip;
import com.extjs.gxt.ui.client.widget.toolbar.PagingToolBar;
import com.extjs.gxt.ui.client.widget.toolbar.SeparatorToolItem;
import com.extjs.gxt.ui.client.widget.toolbar.ToolBar;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class StaticTableViewImpl extends LayoutContainer implements
		StaticTableView {

	private Presenter presenter;
	private Grid<BaseModelData> grid;
	private ColumnModel columnModel;
    private ListStore<BaseModelData> store;
	private ContentPanel cp;
	private boolean showTitleBar;
	private String panelTitle;	

	@Inject
	public StaticTableViewImpl() {
		showTitleBar = true;
		panelTitle = "";	
		cp = new ContentPanel();
		
		// configure a generic initial column model and store. replaced by presenter when Service is first called
        List<ColumnConfig> columns = new ArrayList<ColumnConfig>();
        columns.add(new ColumnConfig("columnId", "Columns", 150));
        columnModel = new ColumnModel(columns);
        store = new ListStore<BaseModelData>();         	
		
		// ContentPanel defaults
		cp.setSize(600, 300); 
		cp.setBodyBorder(true);
		if(showTitleBar) {
			cp.setHeaderVisible(true);
			cp.setHeading(panelTitle);
		} else {
			cp.setHeaderVisible(false);
		}
		cp.setButtonAlign(HorizontalAlignment.CENTER);
		cp.setLayout(new FitLayout());		
	}

	
	@Override
	protected void onRender(Element parent, int index) {
		super.onRender(parent, index);
		FlowLayout layout = new FlowLayout();
		layout.setMargins(new Margins(10, 0, 10, 0));
		setLayout(layout);
		getAriaSupport().setPresentation(true);

		
		
		// grid initial setup
		grid = new Grid<BaseModelData>(store, columnModel);
		grid.setStyleAttribute("borderTop", "none");
		grid.setBorders(false);
		grid.setStripeRows(true);
		grid.setColumnLines(true);
		grid.setColumnReordering(true);
		grid.getAriaSupport().setLabelledBy(cp.getHeader().getId() + "-label");
		grid.setAutoWidth(true);
		grid.setLoadMask(true);
		cp.add(grid);
		
		add(cp);
		// needed to enable quicktips (qtitle for the heading and qtip for the
		// content) that are setup in the change GridCellRenderer
		new QuickTip(grid);
	}

	@Override
	public Widget asWidget() {
		return this;
	}

	@Override
	public void setPresenter(Presenter presenter) {
		this.presenter = presenter;
	}

	@Override
	public void setDataAndColumnsInOrder(List<Map<String, Object>> rows, List<StaticTableColumn> columnsInOrder) {
		store.removeAll();
		addDataToStore(rows);
		createNewColumnObjectModel(columnsInOrder);
		if(grid != null) {			
			grid.reconfigure(store, columnModel);
			grid.show();
			cp.unmask();
		}		
	}	

	@Override
	public void setDataAndColumnOrder(List<Map<String, Object>> rows, List<String> columnOrder) {
		store.removeAll();
		addDataToStore(rows);
		createNewColumnModel(columnOrder);
		if(grid != null) {			
			grid.reconfigure(store, columnModel);
			grid.show();
			cp.unmask();
		}
	}

	
	@Override
	public void setData(List<Map<String, Object>> rows) {
		this.store.removeAll();
		addDataToStore(rows);
		if(grid != null) {			
			grid.reconfigure(store, columnModel);
			grid.show();
			cp.unmask();
		}
	}

	
	@Override
	public void setColumnOrder(List<String> columnOrder) {
		createNewColumnModel(columnOrder);
		if(grid != null) {			
			grid.reconfigure(store, columnModel);
			grid.show();
			cp.unmask();
		}
	}

	@Override
	public void setDimensions(int width, int height) {
		cp.setSize(width, height);
	}

	@Override
	public void showTitleBar(boolean showTitleBar) {
		this.showTitleBar = showTitleBar;
		cp.setHeaderVisible(showTitleBar);
	}


	@Override
	public void setTitleText(String title) {
		this.panelTitle = title;
		cp.setHeading(title);
	}

	
	/*
	 * Private Methods
	 */

	private void createNewColumnModel(List<String> columnsInOrder) {
        List<ColumnConfig> columns = new ArrayList<ColumnConfig>();
        int count = 0;
        for(String colName : columnsInOrder) {
        	if(count >= DisplayConstants.MAX_COLUMNS_IN_GRID) break; // stop displaying columns above a certain number        	
        	columns.add(createColumn(colName, colName, null, null));
        	count++;
        }        
        columnModel = new ColumnModel(columns);		
	}

	private void createNewColumnObjectModel(List<StaticTableColumn> columnsInOrder) {
        List<ColumnConfig> columns = new ArrayList<ColumnConfig>();
        int count = 0;
        for(StaticTableColumn stColumn : columnsInOrder) {
        	if(count >= DisplayConstants.MAX_COLUMNS_IN_GRID) break; // stop displaying columns above a certain number        	
        	columns.add(createColumn(stColumn.getId(), stColumn.getName(), stColumn.getTooltip(), stColumn.getUnits()));
        	count++;
        }        
        columnModel = new ColumnModel(columns);		
	}

	private ColumnConfig createColumn(String id, String colName, String tooltip, String units) {
    	String cleanColName = colName.replaceFirst("\\.", "_");
    	if(units != null) {
    		cleanColName = cleanColName + " <small>(" + units + ")</small>";
    	}
    	ColumnConfig cc = new ColumnConfig(cleanColName, cleanColName, 125);
    	if(tooltip != null) {
    		cc.setToolTip(tooltip);
    	}
    	return cc;
	}
	
	
	private void addDataToStore(List<Map<String, Object>> rows) {
		List<BaseModelData> dataList = new ArrayList<BaseModelData>();
		for(Map<String,Object> rowMap : rows) {									
			BaseModelData dataPt = new BaseModelData();
			for(String key : rowMap.keySet()) {
				String cleanKey = key.replaceFirst("\\.", "_");
				Object value = rowMap.get(key); 								
				dataPt.set(cleanKey, value);
			}
			dataList.add(dataPt);
		}
		this.store.add(dataList);
	}


	@Override
	public void clear() {
		if(grid != null) { 
			grid.hide();
			cp.mask("Loading...");
		}
	}
	
}
