#!/bin/bash

echo
echo "After-Succes Environment:"
echo "  JDK Version: ${TRAVIS_JDK_VERSION}"
echo "  Travis Tag: ${TRAVIS_TAG}"
echo "  Travis Branch: ${TRAVIS_BRANCH}"
echo "  POM Version: ${POM_VERSION}"
echo "  POM SCM Tag: ${POM_SCM_TAG}"
echo

if [[ "${TRAVIS_JDK_VERSION}" != "oraclejdk8" ]]; then
    echo "Skipping after_success actions for JDK version \"${TRAVIS_JDK_VERSION}\""
    exit
fi

mvn -B cobertura:cobertura coveralls:report


# .travis_assert_ready_to_deploy.rb performs all validation to determine if build should be deployed
`ruby .travis_assert_ready_to_deploy.rb`

# An exit status of 0 indicates all validations succeeded
if [[ "${?}" == "0" ]]; then
    printf "\nDeploying tagged version ${POM_VERSION}\n\n"
    bash $DEPLOY_DIR/publish.sh
fi