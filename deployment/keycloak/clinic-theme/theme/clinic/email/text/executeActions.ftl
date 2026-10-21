<#ftl output_format="plainText">
<#assign actions = requiredActions![]>
<#assign invite = actions?seq_contains("VERIFY_EMAIL")>
<#assign reset = !invite && actions?size == 1 && actions?seq_contains("UPDATE_PASSWORD")>
<#assign expires = linkExpirationFormatter(linkExpiration)>
Hello<#if (user.firstName)?has_content> ${user.firstName}</#if>,

<#if invite>
An administrator has created a ${realmName} staff account for you (${user.email}).
To get started, choose your password. This also confirms your email address:
<#elseif reset>
We received a request to reset the password of your ${realmName} account (${user.email}).
Choose a new one here:
<#else>
Please complete the following for your ${realmName} account: <#list actions as action>${msg("requiredAction.${action}")}<#sep>, </#sep></#list>
</#if>

${link}

This link expires in ${expires}.
<#if reset>If you didn't ask for a new password, ignore this email; your current password keeps working.</#if>
<#if invite>If you weren't expecting this, you can ignore this email.</#if>

${realmName}
