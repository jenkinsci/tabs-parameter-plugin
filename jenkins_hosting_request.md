> - https://github.com/YanekC/tabs-parameter/blob/1dcf1db734318e5c9f2c6bd7295ce892f49fd5e7/src/main/resources/io/jenkins/plugins/TabsGroupParameterDefinition/config.jelly#L6 This is missing a `<f:repeatableDeleteButton/>` inside so you can delete tabs in a group. Maybe combine with `minimum="1"` to have at least one tab

Ok for the delete button. For the minimum property, it doesn't stop the user to delete al the tabs. Do you know a builtin ways to prevent it ?

> - Is it useful to allow nested tab groups?

Well I guess it could be useful, when you want to have multiple UI paths. But it can make things pretty complex pretty fast. So I don't know, do it at your own risk I guess ? And I did not thoroughly tested it.
Do you think I should explicitly disable the feature ?

> - The buttons to select a tab need spacing

Done. Took the same spacing as the action bar

> - I can give 2 tabs the same name. That might be confusing. It also breaks the UI as the js works with the tab names to select a tab

I'm not sure how to do this. I tried to do it with the FormValidation objects but I can't make it work. Do you have any example I could follow ?

> - It is not clear which tab is selected. Maybe apply the same style as you have for views, just without the `+`. Quick hack:

Done

> - https://github.com/YanekC/tabs-parameter/blob/1dcf1db734318e5c9f2c6bd7295ce892f49fd5e7/src/main/resources/io/jenkins/plugins/TabsGroupParameterDefinition/tabs.js#L14 This is a bad way to modify the classes. Better use `tabLinks[i].classList.add("active")` and  `tabLinks[i].classList.remove("active")`

Done

> - https://github.com/YanekC/tabs-parameter/blob/1dcf1db734318e5c9f2c6bd7295ce892f49fd5e7/src/main/resources/io/jenkins/plugins/TabsGroupParameterDefinition/tabs.js#L6 This is also a pattern I would avoid. Better use `tabcontent=document.querySelectorAll(".tabcontent")` and the `tabcontent.forEach` to loop over the tabs

Done

> - When I run a `bat` or `sh` step in a pipeline, the parameters inside a tabgroup/tab are not available as environment variables. Would first need to use a `withEnv` step to get the values in.
> -  For a freestyle jobs the parameters are completely unaccessible. There you only have environment variables normally
> -  https://github.com/YanekC/tabs-parameter/blob/1dcf1db734318e5c9f2c6bd7295ce892f49fd5e7/src/main/java/io/jenkins/plugins/TabsGroupParameterDefinition.java#L128 You could validate this in javascript by using `<f:textbox clazz="required" checkMessage="Parameter name must not be empty"/>`, making the `doCheck` method obsolete. Can be applied to other checks as well
> -  https://github.com/YanekC/tabs-parameter/blob/1dcf1db734318e5c9f2c6bd7295ce892f49fd5e7/src/main/java/io/jenkins/plugins/TabParametersDefinition.java#L63 Here I would just add the `@POST` and then suppress the missing permission check
 