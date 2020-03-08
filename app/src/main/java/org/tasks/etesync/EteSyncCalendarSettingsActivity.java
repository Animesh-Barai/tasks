package org.tasks.etesync;

import android.os.Bundle;
import androidx.lifecycle.ViewModelProviders;
import javax.inject.Inject;
import org.tasks.caldav.BaseCaldavCalendarSettingsActivity;
import org.tasks.data.CaldavAccount;
import org.tasks.data.CaldavCalendar;
import org.tasks.injection.ActivityComponent;

public class EteSyncCalendarSettingsActivity extends BaseCaldavCalendarSettingsActivity {

  @Inject EteSyncClient client;
  private CreateCalendarViewModel createCalendarViewModel;
  private DeleteCalendarViewModel deleteCalendarViewModel;
  private UpdateCalendarViewModel updateCalendarViewModel;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    createCalendarViewModel = ViewModelProviders.of(this).get(CreateCalendarViewModel.class);
    deleteCalendarViewModel = ViewModelProviders.of(this).get(DeleteCalendarViewModel.class);
    updateCalendarViewModel = ViewModelProviders.of(this).get(UpdateCalendarViewModel.class);

    createCalendarViewModel.observe(this, this::createSuccessful, this::requestFailed);
    deleteCalendarViewModel.observe(this, this::onDeleted, this::requestFailed);
    updateCalendarViewModel.observe(this, uid -> updateAccount(), this::requestFailed);
  }

  @Override
  protected void createCalendar(CaldavAccount caldavAccount, String name, int color) {
    createCalendarViewModel.createCalendar(client, caldavAccount, name, color);
  }

  @Override
  protected void updateNameAndColor(CaldavAccount account, CaldavCalendar calendar, String name,
      int color) {
    updateCalendarViewModel.updateCalendar(client, account, calendar, name, color);
  }

  @Override
  protected void deleteCalendar(CaldavAccount caldavAccount, CaldavCalendar caldavCalendar) {
    deleteCalendarViewModel.deleteCalendar(client, caldavAccount, caldavCalendar);
  }

  @Override
  public void inject(ActivityComponent component) {
    component.inject(this);
  }
}
