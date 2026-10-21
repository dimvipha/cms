<#ftl output_format="HTML">
<#-- Sent for POST /api/v1/staff (invite: UPDATE_PASSWORD + VERIFY_EMAIL) and for
     password resets (UPDATE_PASSWORD only). -->
<#assign actions = requiredActions![]>
<#assign invite = actions?seq_contains("VERIFY_EMAIL")>
<#assign reset = !invite && actions?size == 1 && actions?seq_contains("UPDATE_PASSWORD")>
<#assign expires = linkExpirationFormatter(linkExpiration)>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>${realmName}</title>
</head>
<body style="margin:0;padding:0;background:#f3f5f7;font-family:Arial,Helvetica,sans-serif;color:#1f2933;">
<table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#f3f5f7;padding:24px 12px;">
  <tr>
    <td align="center">
      <table role="presentation" width="100%" cellpadding="0" cellspacing="0"
             style="max-width:560px;background:#ffffff;border-radius:8px;overflow:hidden;">
        <tr>
          <td style="background:#0f766e;padding:20px 28px;color:#ffffff;font-size:20px;font-weight:bold;">
            ${realmName}
          </td>
        </tr>
        <tr>
          <td style="padding:28px;font-size:15px;line-height:1.6;">
            <h1 style="margin:0 0 16px;font-size:22px;color:#0f172a;">
              <#if invite>Welcome to ${realmName}<#elseif reset>Reset your password<#else>Action needed on your account</#if>
            </h1>

            <p style="margin:0 0 16px;">
              Hello<#if (user.firstName)?has_content> ${user.firstName}</#if>,
            </p>

            <#if invite>
              <p style="margin:0 0 16px;">
                An administrator has created a ${realmName} staff account for you
                (<strong>${user.email}</strong>). To get started, choose your password.
                This also confirms your email address.
              </p>
            <#elseif reset>
              <p style="margin:0 0 16px;">
                We received a request to reset the password of your ${realmName} account
                (<strong>${user.email}</strong>). Choose a new one with the button below.
              </p>
            <#else>
              <p style="margin:0 0 16px;">
                Please complete the following for your ${realmName} account:
                <strong><#list actions as action>${msg("requiredAction.${action}")}<#sep>, </#sep></#list></strong>.
              </p>
            </#if>

            <table role="presentation" cellpadding="0" cellspacing="0" style="margin:8px 0 24px;">
              <tr>
                <td style="background:#0f766e;border-radius:6px;">
                  <a href="${link}" target="_blank"
                     style="display:inline-block;padding:12px 24px;color:#ffffff;text-decoration:none;font-weight:bold;">
                    <#if invite>Set up my account<#elseif reset>Choose a new password<#else>Continue</#if>
                  </a>
                </td>
              </tr>
            </table>

            <p style="margin:0 0 16px;color:#52606d;">
              This link expires in ${expires}.
              <#if reset>If you didn't ask for a new password, ignore this email; your current password keeps working.</#if>
              <#if invite>If you weren't expecting this, you can ignore this email.</#if>
            </p>

            <p style="margin:0;color:#7b8794;font-size:13px;">
              Button not working? Copy this link into your browser:<br>
              <a href="${link}" style="color:#0f766e;word-break:break-all;">${link}</a>
            </p>
          </td>
        </tr>
        <tr>
          <td style="background:#f8fafc;padding:16px 28px;color:#7b8794;font-size:12px;">
            Sent by ${realmName}. Please don't reply to this email.
          </td>
        </tr>
      </table>
    </td>
  </tr>
</table>
</body>
</html>
