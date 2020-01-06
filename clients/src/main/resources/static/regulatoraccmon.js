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
                    <b>{this.name()}</b> Account Monitor
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
                            <Col className="namefield">Supplier</Col>
                        </Row>
                    </ListGroup.Item>
                    { this.props.accounts.map(
                        account =>
                            <ListGroup.Item action
                                            xs="auto"
                                            key={account.linearId.id}
                                            active={account.linearId.id ==
                                                this.props.activeAccountId}
                                            onClick={(id) => this.props.onAccountSelect(
                                                account.linearId.id)}>
                                <Row className="my-n1">
                                    <Col className="accountId" xs={5}>{account.linearId.id}</Col>
                                    <Col className="namefield">{account.firstName}</Col>
                                    <Col className="namefield">{account.lastName}</Col>
                                    <Col className="supplierfield">
                                            {getIdentityDisplayName(account.supplier)}</Col>
                                </Row>
                            </ListGroup.Item>
                    )}

                </ListGroup>
                }
                { this.props.accounts.length == 0 &&
                <p>No accounts exist</p>
                }
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
        this.accountSelect = this.accountSelect.bind(this);
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

    accountSelect(id) {
        this.setState({activeAccountId: id})
    }

    finishAction() {
        this.setState({currentAction: "",
                       actionResult: ""})
        this.getAccounts()
    }

    render() {
        return (
            <div>
                <NavigationBar nodeInfo={this.state.nodeInfo} />
                {this.state.initialised == true &&
                <AccountList accounts={this.state.accounts}
                             activeAccountId={this.state.activeAccountId}
                             onAccountSelect={this.accountSelect} />
                }
            </div>
        )
    }
}
