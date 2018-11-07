// @flow
import React from "react";
import type { User } from "@scm-manager/ui-types";
import {
  InputField,
  SubmitButton,
  Notification,
  ErrorNotification
} from "@scm-manager/ui-components";
import * as userValidator from "./userValidation";
import { translate } from "react-i18next";
import { updatePassword } from "./updatePassword";

type Props = {
  user: User,
  t: string => string
};

type State = {
  password: string,
  loading: boolean,
  passwordConfirmationError: boolean,
  validatePasswordError: boolean,
  validatePassword: string,
  error?: Error,
  passwordChanged: boolean
};

class SetUserPassword extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);

    this.state = {
      password: "",
      loading: false,
      passwordConfirmationError: false,
      validatePasswordError: false,
      validatePassword: "",
      passwordChanged: false
    };
  }

  passwordIsValid = () => {
    return !(
      this.state.validatePasswordError || this.state.passwordConfirmationError
    );
  };

  setLoadingState = () => {
    this.setState({
      ...this.state,
      loading: true
    });
  };

  setErrorState = (error: Error) => {
    this.setState({
      ...this.state,
      error: error,
      loading: false
    });
  };

  setSuccessfulState = () => {
    this.setState({
      ...this.state,
      loading: false,
      passwordChanged: true,
      password: "",
      validatePassword: "",
      validatePasswordError: false,
      passwordConfirmationError: false
    });
  };

  submit = (event: Event) => {
    event.preventDefault();
    if (this.passwordIsValid()) {
      const { user } = this.props;
      const { password } = this.state;
      this.setLoadingState();
      updatePassword(user._links.password.href, password)
        .then(result => {
          if (result.error) {
            this.setErrorState(result.error);
          } else {
            this.setSuccessfulState();
          }
        })
        .catch(err => {});
    }
  };

  render() {
    const { t } = this.props;
    const { loading, passwordChanged, error } = this.state;

    let message = null;

    if (passwordChanged) {
      message = (
        <Notification
          type={"success"}
          children={t("password.set-password-successful")}
          onClose={() => this.onClose()}
        />
      );
    } else if (error) {
      message = <ErrorNotification error={error} />;
    }

    return (
      <form onSubmit={this.submit}>
        {message}
        <InputField
          label={t("user.password")}
          type="password"
          onChange={this.handlePasswordChange}
          value={this.state.password ? this.state.password : ""}
          validationError={this.state.validatePasswordError}
          errorMessage={t("validation.password-invalid")}
          helpText={t("help.passwordHelpText")}
        />
        <InputField
          label={t("validation.validatePassword")}
          type="password"
          onChange={this.handlePasswordValidationChange}
          value={this.state ? this.state.validatePassword : ""}
          validationError={this.state.passwordConfirmationError}
          errorMessage={t("validation.passwordValidation-invalid")}
          helpText={t("help.passwordConfirmHelpText")}
        />
        <SubmitButton
          disabled={!this.passwordIsValid()}
          loading={loading}
          label={t("user-form.submit")}
        />
      </form>
    );
  }

  handlePasswordChange = (password: string) => {
    const validatePasswordError = !this.checkPasswords(
      password,
      this.state.validatePassword
    );
    this.setState({
      validatePasswordError: !userValidator.isPasswordValid(password),
      passwordConfirmationError: validatePasswordError,
      password: password
    });
  };

  handlePasswordValidationChange = (validatePassword: string) => {
    const passwordConfirmed = this.checkPasswords(
      this.state.password,
      validatePassword
    );
    this.setState({
      validatePassword,
      passwordConfirmationError: !passwordConfirmed
    });
  };

  checkPasswords = (password1: string, password2: string) => {
    return password1 === password2;
  };

  onClose = () => {
    this.setState({
      ...this.state,
      passwordChanged: false
    });
  };
}

export default translate("users")(SetUserPassword);