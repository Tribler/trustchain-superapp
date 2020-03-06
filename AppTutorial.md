# Adding your own app to the TrustChain Super App

You can follow these steps to add a new module to the super app. You can also duplicate an existing module (e.g., `trustchain-explorer` or `debug`) and update it according to your needs. However, make sure you still perform the required changes according to this guide.

1. Create a new Android library module (`File` - `New` - `New Module...` - `Android Library`).

2. Define the dependency on `common` module in `build.gradle` dependencies of your module: `implementation project(':common')`.

3. Create a `Fragment` for your application.

4. Create a navigation graph in `src/main/res/navigation` and define your `Fragment` as a start destination. Make sure to use a unique name for your navigation graph file to prevent collisions with other apps.

5. Add an item for your app to the navigation drawer in `common/src/main/res/menu/drawer.xml`. 

6. Create your own `Activity` extending `DrawerActivity`, and override:
   - `navigationGraph` to be the ID of your navigation graph,
   - `topLevelDestinationIds` to be the IDs of top level destinations (it used to determine for which destinations the back button should be shown),
   - `drawerNavigationItem` to be the ID of your drawer navigation item defined in the previous step,
   - and optionally, `bottomNavigationMenu` can return the menu ID to be used for the bottom navigation.

7. Define dependency of the super app on your app module by adding `implementation project(':my-app')` to `app/build.gradle` dependencies, where `my-app` is the name of your module.

8. Define your `Activity` in `app/src/main/AndroidManifest.xml`.

9. Add a handler for a navigation drawer item click in `TrustChainApplication.onNavigationItemSelected`. When your item is selected, your `Activity` should be started. 

10. Install the super app and check that your app is available in the menu and is opened on click: ``./gradlew :app:installDebug``
