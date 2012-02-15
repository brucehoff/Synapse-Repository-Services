package org.sagebionetworks.web.client.view;

import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.IconsImageBundle;
import org.sagebionetworks.web.client.SageImageBundle;
import org.sagebionetworks.web.client.place.ComingSoon;
import org.sagebionetworks.web.client.widget.filter.QueryFilter;
import org.sagebionetworks.web.client.widget.footer.Footer;
import org.sagebionetworks.web.client.widget.header.Header;
import org.sagebionetworks.web.client.widget.search.HomeSearchBox;

import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class HomeViewImpl extends Composite implements HomeView {

	public interface HomeViewImplUiBinder extends UiBinder<Widget, HomeViewImpl> {}
	
	@UiField
	SimplePanel header;
	@UiField
	SimplePanel footer;
	@UiField 
	Anchor demoCharlesLink;
	@UiField
	Anchor seeAllContributors;
	@UiField
	SimplePanel bigSearchBox;
	
	private Presenter presenter;
	private Header headerWidget;
	private Footer footerWidget;
	private GlobalApplicationState globalApplicationState;
	private HomeSearchBox homeSearchBox;
	
	@Inject
	public HomeViewImpl(HomeViewImplUiBinder binder, Header headerWidget, Footer footerWidget, IconsImageBundle icons, QueryFilter filter, SageImageBundle imageBundle, GlobalApplicationState globalApplicationState, HomeSearchBox homeSearchBox) {		
		initWidget(binder.createAndBindUi(this));
		this.headerWidget = headerWidget;
		this.footerWidget = footerWidget;
		this.globalApplicationState = globalApplicationState;
		this.homeSearchBox = homeSearchBox;
		
		header.add(headerWidget.asWidget());
		footer.add(footerWidget.asWidget());
		
		seeAllContributors.addStyleName("bulleted");
		if(DisplayConstants.showDemoHtml) {
			demoCharlesLink.setHref("people_charles.html");
			seeAllContributors.setHref("people.html");
		} else {
			demoCharlesLink.setHref("#" + globalApplicationState.getAppPlaceHistoryMapper().getToken(new ComingSoon(DisplayUtils.DEFAULT_PLACE_TOKEN)));
			seeAllContributors.setHref("#" + globalApplicationState.getAppPlaceHistoryMapper().getToken(new ComingSoon(DisplayUtils.DEFAULT_PLACE_TOKEN)));
		}

		bigSearchBox.clear();
		bigSearchBox.add(homeSearchBox.asWidget());

	}


	@Override
	public void setPresenter(Presenter presenter) {
		this.presenter = presenter;
	}

	@Override
	public void refresh() {
		headerWidget.refresh();
		headerWidget.setSearchVisible(false);
	}

	@Override
	public void showErrorMessage(String message) {
		DisplayUtils.showErrorMessage(message);
	}

	@Override
	public void showLoading() {
	}


	@Override
	public void showInfo(String title, String message) {
		DisplayUtils.showInfo(title, message);
	}

	@Override
	public void clear() {
	}

	
}
