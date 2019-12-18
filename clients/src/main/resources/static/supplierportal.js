"use strict";

let Alert = ReactBootstrap.Alert;
let Accordion = ReactBootstrap.Accordion;
let Button = ReactBootstrap.Button;
let ButtonGroup = ReactBootstrap.ButtonGroup;
let Card = ReactBootstrap.Card;
let Col = ReactBootstrap.Col;
let Container = ReactBootstrap.Container;
let Form = ReactBootstrap.Form;
let InputGroup = ReactBootstrap.InputGroup;
let ListGroup = ReactBootstrap.ListGroup;
let Modal = ReactBootstrap.Modal;
let NavBar = ReactBootstrap.Navbar;
let Row = ReactBootstrap.Row;
let Table = ReactBootstrap.BootstrapTable;
let Text = ReactBootstrap.Text;

function getIdentityDisplayName(X500Name) {
    return X500Name.match("(?<=O=).*(?=, L=)")[0]
}

class NavigationBar extends React.Component {
    constructor(props) {
        super(props);
    }

    name() {
        if (this.props.nodeInfo.legalIdentitiesAndCerts) {
            return getIdentityDisplayName(this.props.nodeInfo.legalIdentitiesAndCerts[0])
        }

        return "";
    }

    render() {
        return (
            <NavBar className="bg-light justify-content-between mb-3">
                <NavBar.Brand>
                    <b>{this.name()}</b> Supplier Portal
                </NavBar.Brand>
                <NavBar.Text>
                    powered by&nbsp;
                    <a href='https://www.r3.com/'>
                        <img src='/corda.png'
                             height="30"
                             className="d-inline-block mb-1"
                             alt='corda'/>
                     </a>
                </NavBar.Text>
            </NavBar>
        )
    }
}

class AccountList extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        return (
            <div>
            <Container fluid>
                { this.props.accounts.length > 0 &&
                <ListGroup className="mb-2 mx-n2">
                    <ListGroup.Item className="bg-light">
                        <Row className="font-weight-bolder my-n1">
                            <Col className="accountId" xs={5}>Account Id</Col>
                            <Col className="namefield">First Name</Col>
                            <Col className="namefield">Last Name</Col>
                        </Row>
                    </ListGroup.Item>
                    { this.props.accounts.map(
                        account =>
                            <ListGroup.Item xs="auto"
                                            key={account.linearId.id}
                                            active={account.linearId.id ==
                                                this.props.activeAccountId}
                                            onClick={(id) => this.props.onAccountSelect(
                                                account.linearId.id)}>
                                <Row className="my-n1">
                                    <Col className="accountId" xs={5}>{account.linearId.id}</Col>
                                    <Col className="namefield">{account.firstName}</Col>
                                    <Col className="namefield">{account.lastName}</Col>
                                </Row>
                            </ListGroup.Item>
                    )}

                </ListGroup>
                }
                { this.props.accounts.length == 0 &&
                <p>No accounts exist</p>
                }
                <ButtonGroup className="mx-n2">
                    <Button variant="dark"
                            onClick={this.props.onAccountCreateRequest}>Create Account</Button>
                    <Button variant="dark"
                            disabled={this.props.activeAccountId == ""}
                            onClick={this.props.onAccountDeleteRequest}>Delete Account</Button>
                </ButtonGroup>
            </Container>
            </div>
        )
    }
}

class AccountForm extends React.Component {
    constructor(props) {
        super(props);

        this.handleFieldChange = this.handleFieldChange.bind(this);
    }

    render() {
        return (
            <Form>
                <Form.Group as={Row}>
                    <Form.Label xs={3}
                                className="font-weight-bolder"
                                column
                                span="false">First Name
                    </Form.Label>
                    <Col>
                        <Form.Control className="nameField"
                                      placeholder="required"
                                      onChange={this.handleFieldChange}
                                      name="firstName"
                                      value={this.props.accountDetails.firstName}/>
                    </Col>
                </Form.Group>
                <Form.Group as={Row}>
                    <Form.Label xs={3}
                                className="font-weight-bolder"
                                column
                                span="false">Last Name
                    </Form.Label>
                    <Col>
                        <Form.Control className="nameField"
                                      placeholder="required"
                                      onChange={this.handleFieldChange}
                                      name="lastName"
                                      value={this.props.accountDetails.lastName}/>
                    </Col>
                </Form.Group>
            </Form>
        )
    }

    handleFieldChange(event) {
        this.props.onChange(event.target.name, event.target.value);
    }
}

class AccountCreateDialog extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            accountDetails: {}
        }

        this.onChange = this.onChange.bind(this);
        this.handleClose = this.handleClose.bind(this);
    }

    render() {
        return (
            <div class="panel panel-default">
                <Modal show={this.props.actionState == "Create"}>
                    <Modal.Header>
                        <Modal.Title>Create Account</Modal.Title>
                    </Modal.Header>
                        <Modal.Body>
                            { this.props.actionResult != "OK" &&
                             <AccountForm accountDetails={this.state.accountDetails}
                                          onChange={this.onChange}/>
                            }
                            { this.props.actionResult == "FAIL" &&
                             <div className="text-danger">Account creation failed</div>
                            }
                            { (this.props.actionResult == "OK") &&
                             "Account was successfully created."
                            }
                        </Modal.Body>
                    <Modal.Footer>
                        { (this.props.actionResult != "OK") &&
                            <ButtonGroup>
                                <Button variant="success"
                                        disabled={this.props.actionResult == "PENDING"}
                                        onClick={() =>
                                            this.props.onCreateConfirm(this.state.accountDetails)}>
                                    Create
                                </Button>
                                <Button variant="secondary" onClick={this.handleClose}>
                                    Cancel
                                </Button>
                            </ButtonGroup>
                        }
                        { (this.props.actionResult == "OK") &&
                              <Button onClick={this.handleClose}>
                                  Close
                              </Button>
                        }
                    </Modal.Footer>
                </Modal>
            </div>
        )
    }

    onChange(field, value) {
        this.setState({accountDetails: Object.assign(
                            {},
                            this.state.accountDetails,
                            {[field]: value})});
    }

    handleClose() {
        this.setState({accountDetails: {}});
        this.props.onClose();
    }
}

class AccountDeleteDialog extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        let modalText = ""

        switch ( this.props.actionResult ) {
        case "":
        case "PENDING":
            modalText = "Are you sure you want to delete account " +
                        this.props.accountId + "?"
            break;
        case "OK":
            modalText = "Account was successfully deleted."
            break;
        case "FAIL":
            modalText = "Account delete failed. Please contact support."
            break;
        default:
            break;
        }

        return (
            <div class="panel panel-default">
                <Modal show={this.props.actionState == "Delete"}>
                    <Modal.Header>
                        <Modal.Title>Delete Account</Modal.Title>
                    </Modal.Header>
                        <Modal.Body>
                            {modalText}
                        </Modal.Body>
                    <Modal.Footer>
                        { (this.props.actionResult == "" ||
                           this.props.actionResult == "PENDING") &&
                            <ButtonGroup>
                                <Button variant="danger"
                                        disabled={this.props.actionResult != ""}
                                        onClick={this.props.onDeleteConfirm}>
                                    Delete
                                </Button>
                                <Button variant="secondary" onClick={this.props.onClose}>
                                    Cancel
                                </Button>
                            </ButtonGroup>
                        }
                        { (this.props.actionResult == "OK" ||
                           this.props.actionResult == "FAIL") &&
                              <Button onClick={this.props.onClose}>
                                  Close
                              </Button>
                        }
                    </Modal.Footer>
                </Modal>
            </div>
        )
    }
}

class App extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            nodeInfo: {},
            networkMap: {},
            accounts: [],
            initialised: false,
            activeAccountId: "",
            currentAction: "",
            actionResult: ""};

        this.getNodeInfo = this.getNodeInfo.bind(this);
        this.getNetworkMap = this.getNetworkMap.bind(this);
        this.accountCreateRequest = this.accountCreateRequest.bind(this);
        this.accountDeleteRequest = this.accountDeleteRequest.bind(this);
        this.accountSelect = this.accountSelect.bind(this);
        this.createAccount = this.createAccount.bind(this);
        this.deleteAccount = this.deleteAccount.bind(this);
        this.finishAction = this.finishAction.bind(this);
    }

    componentDidMount() {
        this.getNodeInfo()
            .then(() => this.getNetworkMap())
            .then(() => this.getAccounts())
    }

    getNodeInfo() {
        return fetch('/api/nodeInfo')
            .then(result => result.json())
            .then(nodeInfo => this.setState({nodeInfo: nodeInfo}));
    }

    getNetworkMap() {
        return fetch('/api/networkMap')
            .then(result => result.json())
            .then(networkMap => this.setState({networkMap: networkMap}));
    }

    getAccounts() {
        return fetch('/api/getAllAccounts')
            .then(result => result.json())
            .then(accounts => this.setState({initialised: true,
                                             accounts: accounts}))
    }

    accountCreateRequest(event) {
        this.setState({currentAction: "Create"})
    }

    accountDeleteRequest(event) {
        this.setState({currentAction: "Delete"})
    }

    accountSelect(id) {
        this.setState({activeAccountId: id})
    }

    createAccount(accountDetails) {
        this.setState({actionResult: "PENDING"})
        return fetch('/api/createAccount', {
            method: 'POST',
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(accountDetails)
        })
            .then(result => result.text())
            .then(result => {
                if ( result == "OK" ) {
                    this.setState({actionResult: "OK"})
                } else {
                    this.setState({actionResult: "FAIL"})
                }
            })
            .then(() => this.getAccounts())
    }

    deleteAccount() {
        this.setState({actionResult: "PENDING"})
        return fetch('/api/deleteAccount', {
            method: 'DELETE',
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                accountId: this.state.activeAccountId
            })
        })
            .then(result => result.text())
            .then(result => {
                if ( result == "OK" ) {
                    this.setState({actionResult: "OK"})
                } else {
                    this.setState({actionResult: "FAIL"})
                }
            })
            .then(() => this.getAccounts())
    }

    finishAction() {
        this.setState({currentAction: "",
                       actionResult: ""})
    }

    render() {
        return (
            <div>
                <NavigationBar nodeInfo={this.state.nodeInfo} />
                {this.state.initialised == true &&
                <AccountList accounts={this.state.accounts}
                             activeAccountId={this.state.activeAccountId}
                             onAccountSelect={this.accountSelect}
                             onAccountCreateRequest={this.accountCreateRequest}
                             onAccountDeleteRequest={this.accountDeleteRequest} />
                }
                <AccountCreateDialog actionState={this.state.currentAction}
                                     actionResult={this.state.actionResult}
                                     onClose={this.finishAction}
                                     onCreateConfirm={(accountDetails) =>
                                         this.createAccount(accountDetails)}/>
                <AccountDeleteDialog actionState={this.state.currentAction}
                                     actionResult={this.state.actionResult}
                                     accountId={this.state.activeAccountId}
                                     onClose={this.finishAction}
                                     onDeleteConfirm={this.deleteAccount}/>
            </div>
        )
    }
}
